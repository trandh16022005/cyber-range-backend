package com.cyberrange.backend.team.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.team.dto.AddTeamMemberRequest;
import com.cyberrange.backend.team.dto.CreateTeamRequest;
import com.cyberrange.backend.team.dto.TeamMemberResponse;
import com.cyberrange.backend.team.dto.TeamResponse;
import com.cyberrange.backend.team.dto.UpdateTeamRequest;
import com.cyberrange.backend.team.service.TeamService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // 1. Get teams with pagination
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TeamResponse>>> getTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<TeamResponse> teams = teamService.getTeams(page, size);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Teams retrieved successfully",
                        teams
                )
        );
    }

    // 2. Get team details
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(
            @PathVariable Long id
    ) {
        TeamResponse team = teamService.getTeamById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Team retrieved successfully",
                        team
                )
        );
    }

    // 3. Create team
    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @RequestBody CreateTeamRequest request
    ) {
        TeamResponse team = teamService.createTeam(request);

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Team created successfully",
                        team
                )
        );
    }

    // 4. Update team
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable Long id,
            @RequestBody UpdateTeamRequest request
    ) {
        TeamResponse team = teamService.updateTeam(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Team updated successfully",
                        team
                )
        );
    }

    // 5. Get team members
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamMembers(
            @PathVariable Long id
    ) {
        List<TeamMemberResponse> members = teamService.getTeamMembers(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Team members retrieved successfully",
                        members
                )
        );
    }

    // 6. Add member to team
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> addTeamMember(
            @PathVariable Long id,
            @RequestBody AddTeamMemberRequest request
    ) {
        TeamMemberResponse member = teamService.addTeamMember(id, request);

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Team member added successfully",
                        member
                )
        );
    }

    // 7. Remove member from team
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeTeamMember(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        teamService.removeTeamMember(id, userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Team member removed successfully",
                        null
                )
        );
    }
}

