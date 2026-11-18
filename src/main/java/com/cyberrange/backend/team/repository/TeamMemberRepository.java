package com.cyberrange.backend.team.repository;

import com.cyberrange.backend.team.entity.TeamMember;
import com.cyberrange.backend.team.entity.TeamMemberId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository
        extends JpaRepository<TeamMember, TeamMemberId> {

    @EntityGraph(attributePaths = {"user", "user.role"})
    List<TeamMember> findByTeam_Id(Long teamId);

    @EntityGraph(attributePaths = {"user", "user.role"})
    List<TeamMember> findByTeam_IdOrderByJoinedAtAsc(Long teamId);

    @EntityGraph(attributePaths = {"user", "user.role"})
    List<TeamMember> findByUser_Id(Long userId);

    boolean existsByTeam_IdAndUser_Id(Long teamId, Long userId);

    void deleteByTeam_IdAndUser_Id(Long teamId, Long userId);

    long countByTeam_Id(Long teamId);
}
