package com.cyberrange.backend.assessment.service;

import com.cyberrange.backend.assessment.entity.Assessment;
import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.exercise.repository.ExerciseParticipantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BlueSshAccess {

    private final ExerciseParticipantRepository participants;
    private final AssessmentRepository assessments;

    public BlueSshAccess(ExerciseParticipantRepository participants, AssessmentRepository assessments) {
        this.participants = participants;
        this.assessments = assessments;
    }

    public static boolean isStudent(String role) {
        return "STUDENT".equalsIgnoreCase(role);
    }

    public static boolean isSsh(Assessment assessment) {
        return SshAssessmentEvaluator.SCENARIO_NAME.equals(assessment.getExercise().getScenario().getName());
    }

    public long requireTeam(long exerciseId, long actorId) {
        return participants.findByExercise_IdAndUser_Id(exerciseId, actorId)
                .filter(member -> "BLUE".equals(member.getRole()) && member.getTeam() != null)
                .filter(member -> SshAssessmentEvaluator.SCENARIO_NAME.equals(member.getExercise().getScenario().getName()))
                .map(member -> member.getTeam().getId())
                .orElseThrow(() -> new AccessDeniedException("BLUE membership is required for this SSH Exercise"));
    }

    public void requireAssessment(long exerciseId, long assessmentId, long actorId) {
        long teamId = requireTeam(exerciseId, actorId);
        if (!assessments.findByExercise_IdAndTeam_Id(exerciseId, teamId)
                .filter(assessment -> assessment.getId() == assessmentId).isPresent()) {
            throw new AccessDeniedException("Assessment is outside your BLUE Team");
        }
    }
}
