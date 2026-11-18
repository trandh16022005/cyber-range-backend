package com.cyberrange.backend.team.service;

import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.common.response.PageResponse;
import com.cyberrange.backend.team.dto.AddTeamMemberRequest;
import com.cyberrange.backend.team.dto.CreateTeamRequest;
import com.cyberrange.backend.team.dto.TeamMemberResponse;
import com.cyberrange.backend.team.dto.TeamResponse;
import com.cyberrange.backend.team.dto.UpdateTeamRequest;
import com.cyberrange.backend.team.entity.Team;
import com.cyberrange.backend.team.entity.TeamMember;
import com.cyberrange.backend.team.repository.TeamMemberRepository;
import com.cyberrange.backend.team.repository.TeamRepository;
import com.cyberrange.backend.user.entity.User;
import com.cyberrange.backend.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    // 1. Get paginated teams
    public PageResponse<TeamResponse> getTeams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<TeamResponse> teams = teamRepository.findAll(pageable)
                .map(TeamResponse::fromEntity);

        return PageResponse.from(teams);
    }

    // 2. Get team by ID
    public TeamResponse getTeamById(Long teamId) {
        Team team = findTeamById(teamId);
        return TeamResponse.fromEntity(team);
    }

    // 3. Create team
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        String name = normalizeName(request.name());
        validateName(name);

        if (teamRepository.existsByName(name)) {
            throw new BusinessException("Team name already exists");
        }

        String description = normalizeDescription(request.description());

        Team team = new Team(name, description);
        Team savedTeam = teamRepository.save(team);

        return TeamResponse.fromEntity(savedTeam);
    }

    // 4. Update team
    @Transactional
    public TeamResponse updateTeam(Long teamId, UpdateTeamRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        if (request.name() == null && request.description() == null) {
            throw new IllegalArgumentException("At least one field must be provided");
        }

        Team team = findTeamById(teamId);

        String name = team.getName();
        String description = team.getDescription();

        if (request.name() != null) {
            name = normalizeName(request.name());
            validateName(name);

            teamRepository.findByName(name).ifPresent(existingTeam -> {
                if (!existingTeam.getId().equals(teamId)) {
                    throw new BusinessException("Team name already exists");
                }
            });
        }

        if (request.description() != null) {
            description = normalizeDescription(request.description());
        }

        team.update(name, description);

        Team updatedTeam = teamRepository.save(team);

        return TeamResponse.fromEntity(updatedTeam);
    }

    // 5. Get team members
    public List<TeamMemberResponse> getTeamMembers(Long teamId) {
        findTeamById(teamId);

        return teamMemberRepository.findByTeam_IdOrderByJoinedAtAsc(teamId)
                .stream()
                .map(TeamMemberResponse::fromEntity)
                .toList();
    }

    // 6. Add user to team
    @Transactional
    public TeamMemberResponse addTeamMember(
            Long teamId,
            AddTeamMemberRequest request
    ) {
        if (request == null || request.userId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        Team team = findTeamById(teamId);
        User user = findUserById(request.userId());

        if (teamMemberRepository.existsByTeam_IdAndUser_Id(
                teamId,
                request.userId()
        )) {
            throw new BusinessException("User is already a member of this team");
        }

        TeamMember teamMember = new TeamMember(team, user);
        TeamMember savedMember = teamMemberRepository.save(teamMember);

        return TeamMemberResponse.fromEntity(savedMember);
    }

    // 7. Remove user from team
    @Transactional
    public void removeTeamMember(Long teamId, Long userId) {
        findTeamById(teamId);
        findUserById(userId);

        if (!teamMemberRepository.existsByTeam_IdAndUser_Id(teamId, userId)) {
            throw new ResourceNotFoundException("Team member not found");
        }

        teamMemberRepository.deleteByTeam_IdAndUser_Id(teamId, userId);
    }

    // 8. Delete team
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = findTeamById(teamId);

        teamRepository.delete(team);
    }

    // Find team or throw 404
    private Team findTeamById(Long teamId) {
        if (teamId == null) {
            throw new IllegalArgumentException("Team ID must not be null");
        }

        return teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Team not found with ID: " + teamId)
                );
    }

    // Find user or throw 404
    private User findUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID: " + userId)
                );
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name is required");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("Team name must not exceed 100 characters");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}

