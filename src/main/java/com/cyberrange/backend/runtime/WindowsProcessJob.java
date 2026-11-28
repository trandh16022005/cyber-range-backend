package com.cyberrange.backend.runtime;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class WindowsProcessJob implements AutoCloseable {
    private static final Kernel32 KERNEL = Kernel32.INSTANCE;
    private static final JobApi JOB_API = Native.load("kernel32", JobApi.class);
    private static final int EXTENDED_LIMIT_INFORMATION = 9;
    private static final int KILL_ON_JOB_CLOSE = 0x2000;
    private static final int CREATE_UNICODE_ENVIRONMENT = 0x400;
    private static final int CREATE_NO_WINDOW = 0x08000000;
    private final List<HANDLE> processes = new ArrayList<>();
    private final HANDLE job;

    WindowsProcessJob() {
        job = JOB_API.CreateJobObjectW(null, null);
        if (job == null) {
            throw lastError();
        }
        var limits = new ExtendedLimits();
        limits.basic.flags = KILL_ON_JOB_CLOSE;
        limits.write();
        if (!JOB_API.SetInformationJobObject(job, EXTENDED_LIMIT_INFORMATION, limits.getPointer(), limits.size())) {
            var error = lastError();
            KERNEL.CloseHandle(job);
            throw error;
        }
    }

    Child start(List<String> command, Path directory, Map<String, String> environment, Path log) {
        var security = new WinBase.SECURITY_ATTRIBUTES();
        security.bInheritHandle = true;
        var output = KERNEL.CreateFile(log.toString(), WinNT.GENERIC_WRITE, WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE,
                security, WinNT.CREATE_NEW, WinNT.FILE_ATTRIBUTE_NORMAL, null);
        if (WinBase.INVALID_HANDLE_VALUE.equals(output)) {
            throw lastError();
        }
        try {
            var input = KERNEL.CreateFile("NUL", WinNT.GENERIC_READ, WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE,
                    security, WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL, null);
            if (WinBase.INVALID_HANDLE_VALUE.equals(input)) {
                throw lastError();
            }
            try {
                return start(command, directory, environment, input, output);
            } finally {
                KERNEL.CloseHandle(input);
            }
        } finally {
            KERNEL.CloseHandle(output);
        }
    }

    private Child start(List<String> command, Path directory, Map<String, String> environment, HANDLE input, HANDLE output) {
        var startup = new WinBase.STARTUPINFO();
        startup.dwFlags = WinBase.STARTF_USESTDHANDLES;
        startup.hStdInput = input;
        startup.hStdOutput = output;
        startup.hStdError = output;
        var process = new WinBase.PROCESS_INFORMATION();
        int flags = WinBase.CREATE_SUSPENDED | CREATE_UNICODE_ENVIRONMENT | CREATE_NO_WINDOW;
        String commandLine = String.join(" ", command.stream().map(WindowsProcessJob::quote).toList());
        try (var block = environmentBlock(environment)) {
            try {
                if (!KERNEL.CreateProcessW(command.getFirst(), (commandLine + '\0').toCharArray(), null, null, true,
                        new DWORD(flags), block, directory.toString(), startup, process)) {
                    throw lastError();
                }
            } finally {
                block.clear();
            }
        }
        try {
            // The child cannot spawn descendants before Windows has placed it in the job.
            if (!JOB_API.AssignProcessToJobObject(job, process.hProcess) || JOB_API.ResumeThread(process.hThread) == -1) {
                throw lastError();
            }
            processes.add(process.hProcess);
            return new Child(process.hProcess, process.dwProcessId.longValue());
        } catch (RuntimeException error) {
            KERNEL.TerminateProcess(process.hProcess, 1);
            KERNEL.CloseHandle(process.hProcess);
            throw error;
        } finally {
            KERNEL.CloseHandle(process.hThread);
        }
    }

    private static Memory environmentBlock(Map<String, String> environment) {
        var sorted = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(environment);
        var contents = new StringBuilder();
        sorted.forEach((key, value) -> contents.append(key).append('=').append(value).append('\0'));
        contents.append('\0');
        var memory = new Memory((long) contents.length() * Native.WCHAR_SIZE);
        for (int i = 0; i < contents.length(); i++) {
            memory.setChar((long) i * Native.WCHAR_SIZE, contents.charAt(i));
        }
        return memory;
    }

    static String quote(String argument) {
        var result = new StringBuilder("\"");
        int slashes = 0;
        for (int i = 0; i < argument.length(); i++) {
            char character = argument.charAt(i);
            if (character == '\\') {
                slashes++;
                continue;
            }
            result.append("\\".repeat(character == '"' ? 2 * slashes + 1 : slashes)).append(character);
            slashes = 0;
        }
        return result.append("\\".repeat(2 * slashes)).append('"').toString();
    }

    private static Win32Exception lastError() {
        return new Win32Exception(KERNEL.GetLastError());
    }

    @Override
    public void close() {
        KERNEL.CloseHandle(job);
        for (var process : processes) {
            KERNEL.WaitForSingleObject(process, 2000);
            KERNEL.CloseHandle(process);
        }
    }

    record Child(HANDLE handle, long pid) {
        boolean alive() {
            return KERNEL.WaitForSingleObject(handle, 0) == WinError.WAIT_TIMEOUT;
        }

        int exitCode() {
            var result = new IntByReference();
            return KERNEL.GetExitCodeProcess(handle, result) ? result.getValue() : -1;
        }
    }

    public interface JobApi extends StdCallLibrary {
        HANDLE CreateJobObjectW(Pointer attributes, Pointer name);
        boolean SetInformationJobObject(HANDLE job, int informationClass, Pointer information, int bytes);
        boolean AssignProcessToJobObject(HANDLE job, HANDLE process);
        int ResumeThread(HANDLE thread);
    }

    @Structure.FieldOrder({"processTime", "jobTime", "flags", "minimumWorkingSet", "maximumWorkingSet",
            "activeProcessLimit", "affinity", "priorityClass", "schedulingClass"})
    public static class BasicLimits extends Structure {
        public long processTime;
        public long jobTime;
        public int flags;
        public SIZE_T minimumWorkingSet;
        public SIZE_T maximumWorkingSet;
        public int activeProcessLimit;
        public SIZE_T affinity;
        public int priorityClass;
        public int schedulingClass;
    }

    @Structure.FieldOrder({"basic", "readOperations", "writeOperations", "otherOperations", "readBytes", "writeBytes",
            "otherBytes", "processMemory", "jobMemory", "peakProcessMemory", "peakJobMemory"})
    public static class ExtendedLimits extends Structure {
        public BasicLimits basic;
        public long readOperations;
        public long writeOperations;
        public long otherOperations;
        public long readBytes;
        public long writeBytes;
        public long otherBytes;
        public SIZE_T processMemory;
        public SIZE_T jobMemory;
        public SIZE_T peakProcessMemory;
        public SIZE_T peakJobMemory;
    }
}
