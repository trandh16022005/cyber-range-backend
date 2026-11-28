#Requires -Version 5.1
[CmdletBinding(DefaultParameterSetName = 'Start')]
param(
    [Parameter(Mandatory, ParameterSetName = 'Check')]
    [switch] $Check,

    [Parameter(ParameterSetName = 'Start')]
    [switch] $Background
)

$ErrorActionPreference = 'Stop'
$backendUrl = 'http://127.0.0.1:8080'
$frontendUrl = 'http://127.0.0.1:5173'
$databaseUrl = 'jdbc:postgresql://127.0.0.1:5432/cyber_range_db?currentSchema=public'
$labCompose = Join-Path $PSScriptRoot 'cyber-range-lab\compose.yaml'
$tokenPath = Join-Path $env:LOCALAPPDATA 'CyberRange\service-tokens.json'
$tokenNames = @('SSH_LAB_RUNNER_TOKEN', 'SSH_LAB_COLLECTOR_TOKEN')

function Test-TcpPort($Port) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $connection = $client.ConnectAsync('127.0.0.1', $Port)
        return $connection.Wait(1500) -and $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Assert-Prerequisites {
    foreach ($name in 'java.exe', 'python.exe', 'docker.exe', 'node.exe', 'npm.cmd') {
        if (-not (Get-Command $name -CommandType Application -ErrorAction SilentlyContinue)) {
            throw "$name is missing from PATH. See README.md."
        }
    }
    if ((& java.exe --version | Select-Object -First 1) -notmatch '\b21(?:\.|\s|$)') {
        throw 'Java 21 is required. Check java --version and JAVA_HOME.'
    }
    & python.exe --version | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Python could not start. Check python --version.' }

    $dockerEndpoint = $env:DOCKER_HOST
    if ($env:DOCKER_CONTEXT -or -not $dockerEndpoint) {
        $dockerEndpoint = & docker.exe context inspect --format '{{.Endpoints.docker.Host}}'
        if ($LASTEXITCODE -ne 0) { throw 'Cannot read the current Docker context.' }
    }
    if ($dockerEndpoint -notmatch '^npipe:/{2,}\.(/|\\)pipe(/|\\)') {
        throw 'Select the local Docker Desktop context. The launcher requires a local named pipe.'
    }
    & docker.exe info --format '{{.ServerVersion}}' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop is not ready. Start it and retry.' }
    & docker.exe compose -f $labCompose config -q
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose configuration check failed.' }

    if (-not (Test-TcpPort 5432)) { throw 'PostgreSQL is not reachable at 127.0.0.1:5432.' }
    if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'frontend\node_modules\.bin\vite.cmd'))) {
        throw 'Frontend dependencies are missing. Run npm.cmd ci in frontend first.'
    }
    Write-Host 'Preflight OK: Java 21, Python, local Docker Compose, PostgreSQL TCP and Vite.'
}

function Get-PlainText([Security.SecureString] $Secret) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secret)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Read-ServiceTokens([switch] $Create) {
    if (-not (Test-Path -LiteralPath $tokenPath)) {
        if (-not $Create) { throw 'Service token store is missing. Run the launcher without -Check once.' }

        $encrypted = [ordered]@{}
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        $bytes = New-Object byte[] 32
        try {
            foreach ($name in $tokenNames) {
                $random.GetBytes($bytes)
                $secret = ConvertTo-SecureString ([Convert]::ToBase64String($bytes)) -AsPlainText -Force
                try { $encrypted[$name] = ConvertFrom-SecureString $secret } finally { $secret.Dispose() }
            }
        } finally {
            [Array]::Clear($bytes, 0, $bytes.Length)
            $random.Dispose()
        }
        $directory = Split-Path -Parent $tokenPath
        [IO.Directory]::CreateDirectory($directory) | Out-Null
        $tempPath = Join-Path $directory ([Guid]::NewGuid().ToString('N') + '.tmp')
        try {
            [IO.File]::WriteAllText($tempPath, ($encrypted | ConvertTo-Json -Compress))
            [IO.File]::Move($tempPath, $tokenPath)
        } finally {
            [IO.File]::Delete($tempPath)
        }
        Write-Host 'Created the current Windows account DPAPI service token store.'
    }

    $tokens = @{}
    try {
        $store = Get-Content -LiteralPath $tokenPath -Raw | ConvertFrom-Json
        foreach ($name in $tokenNames) {
            $secret = ConvertTo-SecureString -String $store.$name
            try { $tokens[$name] = Get-PlainText $secret } finally { $secret.Dispose() }
            if ($tokens[$name] -notmatch '^[A-Za-z0-9+/=]{32,}$') { throw 'Invalid token format.' }
        }
        if ($tokens[$tokenNames[0]] -ceq $tokens[$tokenNames[1]]) { throw 'Tokens must be distinct.' }
    } catch {
        throw 'Cannot read the service tokens. Use the original Windows account, or follow the rotation steps in README.md.'
    }
    return $tokens
}

function Read-BackendSecret([string] $Name) {
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ($value) { return $value }
    if ([Console]::IsInputRedirected -and $Background) {
        throw "Set $Name in the current process before using -Background without an interactive terminal."
    }
    $secret = Read-Host $Name -AsSecureString
    try { return Get-PlainText $secret } finally { $secret.Dispose() }
}

function Assert-JwtSecret([string] $Value) {
    try { $bytes = [Convert]::FromBase64String($Value) } catch { $bytes = @() }
    try {
        if ($bytes.Length -lt 32) { throw 'JWT_SECRET must be Base64 with at least 32 decoded bytes.' }
    } finally {
        [Array]::Clear($bytes, 0, $bytes.Length)
    }
}

function Get-LocalResponse([string] $Url, [string] $Token) {
    $request = [Net.HttpWebRequest]::Create($Url)
    $request.Proxy = $null
    $request.AllowAutoRedirect = $false
    $request.Timeout = 3000
    $request.ReadWriteTimeout = 3000
    if ($Token) { $request.Headers['X-Lab-Credential'] = $Token }
    try {
        $response = $request.GetResponse()
    } catch [Net.WebException] {
        $response = $_.Exception.Response
        if (-not $response) { return [pscustomobject]@{ Code = 0; Json = $null } }
    }
    try {
        $json = $null
        if ($response.StatusCode -eq 200 -and $Token) {
            $reader = New-Object IO.StreamReader($response.GetResponseStream())
            try { $json = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        }
        return [pscustomobject]@{ Code = [int] $response.StatusCode; Json = $json }
    } finally {
        $response.Dispose()
    }
}

function Wait-WebService($Process, [string] $Name, [string] $Url, [int] $Seconds, [string] $Token) {
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    do {
        $response = Get-LocalResponse $Url $Token
        if ($response.Code -eq 200 -and (-not $Token -or $response.Json.success)) { return $response }
        if ($response.Code -ne 0) { throw "$Name returned HTTP $($response.Code). See README.md and the component logs." }
        if ($Process.HasExited) { throw "$Name exited during startup. Check the component logs." }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "$Name startup timed out. Check the component logs."
}

function Start-Component([string] $Name, [string] $Command, [string] $Directory, [hashtable] $Variables) {
    $commandText = '$Host.UI.RawUI.WindowTitle = "Cyber Range - ' + $Name + '"; ' + $Command + '; exit $LASTEXITCODE'
    if (-not $Background) {
        $commandText = '$Host.UI.RawUI.WindowTitle = "Cyber Range - ' + $Name + '"; ' + $Command + '; Write-Host "Process ended. Review the output before closing this window."'
    }
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($commandText))
    $arguments = @('-NoLogo', '-NoProfile')
    $options = @{ WorkingDirectory = $Directory; PassThru = $true }
    if ($Background) {
        $logDir = Join-Path $PSScriptRoot 'target\startup-logs'
        [IO.Directory]::CreateDirectory($logDir) | Out-Null
        $logPrefix = Join-Path $logDir ($Name + '-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmssfff'))
        $options.WindowStyle = 'Hidden'
        $options.RedirectStandardOutput = "$logPrefix.out.log"
        $options.RedirectStandardError = "$logPrefix.err.log"
        Write-Host "$Name logs: $logPrefix.*.log"
    } else {
        $arguments += '-NoExit'
    }
    $previous = @{}
    try {
        foreach ($key in $Variables.Keys) {
            $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
            [Environment]::SetEnvironmentVariable($key, $Variables[$key], 'Process')
        }
        $arguments += @('-EncodedCommand', $encoded)
        return Start-Process (Join-Path $PSHOME 'powershell.exe') -ArgumentList $arguments @options
    } finally {
        foreach ($key in $previous.Keys) {
            [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process')
        }
    }
}

function Show-LabStatus($Session, [string] $CollectorToken) {
    if (-not $Session) {
        Write-Host 'No active Lab session. An idle runner has no session heartbeat or Collector health endpoint.'
        return
    }
    $sessionId = ([Guid] $Session.id).ToString()
    $project = "cyber-range-ssh-$sessionId"
    $containers = @(& docker.exe compose -f $labCompose -p $project ps --all --format '{{.Service}} | {{.State}} | {{.Health}}')
    if ($LASTEXITCODE -ne 0) { throw "Cannot inspect the Compose project $project." }
    Write-Host "Exercise $($Session.exerciseId), Lab $sessionId, stored=$($Session.status), desiredRunning=$($Session.desiredRunning)."
    if ($containers.Count) { $containers | ForEach-Object { Write-Host $_ } } else { Write-Host 'No containers found for this session.' }
    if ($Session.heartbeatAt) {
        $age = [DateTimeOffset]::UtcNow - [DateTimeOffset] $Session.heartbeatAt
        if ($age.TotalSeconds -ge 30) { Write-Warning "Lab heartbeat is stale ($([math]::Floor($age.TotalSeconds)) s)." }
        else { Write-Host "Lab heartbeat is fresh ($([math]::Max(0, [math]::Floor($age.TotalSeconds))) s)." }
    } else {
        Write-Warning 'This Lab session has not reported a heartbeat.'
    }
    if (-not $Session.desiredRunning) {
        Write-Warning 'Stop is pending until the runner acknowledges STOPPED.'
        return
    }
    $health = Get-LocalResponse "$backendUrl/api/v1/lab-agent/$sessionId/collector-health" $CollectorToken
    if ($health.Code -eq 200 -and $health.Json.success) {
        Write-Host 'Backend accepted the Collector token. See the Lab workflow for end-to-end Collector health.'
    } else {
        Write-Warning "Collector credential check returned HTTP $($health.Code). See README.md."
    }
    $runningCount = @($containers | Where-Object { $_ -match '\| running \|' }).Count
    if ($Session.status -eq 'RUNNING' -and $runningCount -ne 3) {
        Write-Warning 'Stored RUNNING status disagrees with the three expected running containers.'
    }
}

$mutex = New-Object Threading.Mutex($false, 'Local\CyberRange.Main.Startup')
$locked = $false
try {
    if (-not $Check) {
        try { $locked = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
        if (-not $locked) { throw 'Another Cyber Range startup is in progress.' }
    }
    Assert-Prerequisites
    $desiredUrl = "$backendUrl/api/v1/lab-agent/desired"

    if ($Check) {
        $tokens = Read-ServiceTokens
        Write-Host 'DPAPI token store: OK.'
        $response = Get-LocalResponse $desiredUrl $tokens['SSH_LAB_RUNNER_TOKEN']
        if ($response.Code -ne 200 -or -not $response.Json.success) {
            throw "Backend Runner check failed (HTTP $($response.Code)). See README.md."
        }
        Write-Host 'Backend and Runner authentication: OK.'
        Show-LabStatus $response.Json.data $tokens['SSH_LAB_COLLECTOR_TOKEN']
        if ((Get-LocalResponse $frontendUrl).Code -eq 200) { Write-Host "Frontend: $frontendUrl" }
        else { Write-Warning "Frontend is not ready at $frontendUrl." }
        return
    }

    foreach ($port in 8080, 5173) {
        $listeners = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
        if ($listeners.Count -gt 0 -or (Test-TcpPort $port)) {
            $owners = ($listeners.OwningProcess | Sort-Object -Unique) -join ', '
            throw "Port $port is in use. Listening PID(s): $owners. Stop the intended process first."
        }
    }
    if ($env:SPRING_APPLICATION_JSON -or $env:SPRING_CONFIG_LOCATION -or $env:SPRING_CONFIG_ADDITIONAL_LOCATION) {
        throw 'Remove custom Spring configuration overrides in this terminal before using the main launcher.'
    }
    $dbPassword = Read-BackendSecret 'DATABASE_PASSWORD'
    if ([string]::IsNullOrEmpty($dbPassword)) { throw 'DATABASE_PASSWORD is required.' }
    $jwtSecret = Read-BackendSecret 'JWT_SECRET'
    Assert-JwtSecret $jwtSecret
    $tokens = Read-ServiceTokens -Create
    $dbUser = $env:DATABASE_USERNAME
    if (-not $dbUser) { $dbUser = 'postgres' }

    $backend = Start-Component 'Backend' '& .\mvnw.cmd spring-boot:run' $PSScriptRoot ($tokens + @{
        SERVER_ADDRESS = '127.0.0.1'; SERVER_PORT = '8080';
        DATABASE_URL = $databaseUrl; DATABASE_USERNAME = $dbUser; DATABASE_PASSWORD = $dbPassword;
        SPRING_DATASOURCE_URL = $databaseUrl; SPRING_DATASOURCE_USERNAME = $dbUser;
        SPRING_DATASOURCE_PASSWORD = $dbPassword; SPRING_PROFILES_ACTIVE = $null;
        SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'; SPRING_SQL_INIT_MODE = 'never';
        JWT_SECRET = $jwtSecret
    })
    $first = Wait-WebService $backend 'Backend' $desiredUrl 180 $tokens['SSH_LAB_RUNNER_TOKEN']
    Write-Host "Backend: $backendUrl (Runner token accepted)."

    $withoutBackendSecrets = @{
        DATABASE_PASSWORD = $null; SPRING_DATASOURCE_PASSWORD = $null; JWT_SECRET = $null
    }
    $runner = Start-Component 'Runner' '& python.exe -B cyber-range-lab/runner.py' $PSScriptRoot ($tokens + $withoutBackendSecrets + @{
        SSH_LAB_BACKEND_PORT = '8080'
    })
    $frontend = Start-Component 'Frontend' '& npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort' (Join-Path $PSScriptRoot 'frontend') ($withoutBackendSecrets + @{
        SSH_LAB_RUNNER_TOKEN = $null; SSH_LAB_COLLECTOR_TOKEN = $null;
        VITE_BACKEND_PROXY_TARGET = $backendUrl
    })
    $null = Wait-WebService $frontend 'Frontend' $frontendUrl 45
    Write-Host "Frontend: $frontendUrl. Runner PID: $($runner.Id)."

    $session = $first.Json.data
    if ($session -and -not $session.desiredRunning) {
        $stoppingId = $session.id
        $deadline = [DateTime]::UtcNow.AddSeconds(60)
        do {
            if ($runner.HasExited) { throw 'Runner exited. Check its startup logs.' }
            $response = Get-LocalResponse $desiredUrl $tokens['SSH_LAB_RUNNER_TOKEN']
            if ($response.Code -eq 200 -and $response.Json.success) {
                $session = $response.Json.data
                if (-not $session -or $session.id -ne $stoppingId) {
                    Write-Host "Runner acknowledged STOPPED for $stoppingId."
                    break
                }
            }
            Start-Sleep -Seconds 2
        } while ([DateTime]::UtcNow -lt $deadline)
    }
    Show-LabStatus $session $tokens['SSH_LAB_COLLECTOR_TOKEN']
    Write-Host 'For another status check, run .\Start-CyberRange.ps1 -Check.'
} finally {
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
