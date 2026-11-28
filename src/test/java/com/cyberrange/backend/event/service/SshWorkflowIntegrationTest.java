package com.cyberrange.backend.event.service;

import com.cyberrange.backend.assessment.service.AssessmentService;
import com.cyberrange.backend.assessment.service.SshAssessmentEvaluator;
import com.cyberrange.backend.assessment.service.SshAssessmentEvaluator.EvaluationRequest;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.event.service.SshObservationService.Observation;
import com.cyberrange.backend.event.service.SshResponseWorkflow.Command;
import com.cyberrange.backend.event.service.SshResponseWorkflow.ProbeResult;
import com.cyberrange.backend.lab.service.SshLabService;
import com.cyberrange.backend.lab.service.SshLabService.Health;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:554(?:39|40)/cyber_range_test")
class SshWorkflowIntegrationTest {

    @Autowired JdbcClient db;
    @Autowired MockMvc mvc;
    @Autowired AssessmentService assessments;
    @Autowired SshAssessmentEvaluator evaluator;
    @Autowired SshLabService lab;
    @Autowired SshObservationService ingestion;
    @Autowired SshResponseWorkflow workflow;

    @Test
    void completesVerifiedWorkflowAndRerunsScoringWithoutDuplicatingEvidence() throws Exception {
        long exercise = id("""
                INSERT INTO exercises (scenario_id, cyber_range_id, name, status)
                SELECT scenario_id, cyber_range_id, 'SSH Integration Test Transaction', 'CREATED'
                FROM exercises WHERE name = 'SSH Fixture Exercise' RETURNING id
                """);
        db.sql("""
                INSERT INTO exercise_participants (exercise_id, user_id, team_id, role)
                SELECT :exercise, p.user_id, p.team_id, p.role FROM exercise_participants p
                JOIN exercises e ON e.id = p.exercise_id WHERE e.name = 'SSH Fixture Exercise'
                """).param("exercise", exercise).update();
        long team = id("SELECT id FROM teams WHERE name = 'SSH Fixture Blue'");
        long actor = id("SELECT id FROM users WHERE username = 'ssh-fixture-admin'");
        mvc.perform(get("/api/v1/exercises/{id}/ssh-lab", exercise)).andExpect(status().isUnauthorized());
        var outsider = jwt().jwt(token -> token.subject("987654").claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
        mvc.perform(get("/api/v1/exercises/{id}/ssh-lab", exercise).with(outsider)).andExpect(status().isForbidden());
        var blue = jwt().jwt(token -> token.subject(Long.toString(actor)).claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
        mvc.perform(get("/api/v1/lab-agent/desired").header("X-Lab-Credential", "invalid")).andExpect(status().isForbidden());

        assertEquals(7, assessments.getTaskCriteriaStatus(exercise).stream().filter(AssessmentService.TaskCriteriaStatus::configured).count());
        long assessment = assessments.initializeAssessments(exercise, team).getFirst().id();
        assertEquals(assessment, assessments.initializeAssessments(exercise, team).getFirst().id());
        assertTrue(assessments.getMyAssessments(Long.toString(actor), "STUDENT").stream()
                .anyMatch(item -> item.id() == assessment));
        mvc.perform(get("/api/v1/assessments/{id}/results", assessment).with(blue)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/assessments/{id}/results", assessment).with(outsider)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/exercises/{id}/ssh-lab/stop", exercise).with(blue)).andExpect(status().isForbidden());
        var session = lab.start(exercise, assessment);
        assertFalse(session.ready());
        assertEquals(session.id(), lab.start(exercise, assessment).id());
        String source = "192.0.2.42";
        lab.heartbeat(session.id(), "RUNNING", new Health(true, true, true, true, true, true, source, null));
        assertTrue(lab.status(exercise).ready());

        db.sql("UPDATE ssh_lab_sessions SET created_at = clock_timestamp() - interval '15 seconds' WHERE id = :id")
                .param("id", session.id()).update();
        var entries = new ClassPathResource("ssh/auth-failures.log").getContentAsString(StandardCharsets.UTF_8)
                .lines().filter(line -> !line.startsWith("#")).toList();
        var observations = new ArrayList<Observation>();
        var reference = Instant.now().minusSeconds(8);
        for (int i = 0; i < entries.size(); i++) {
            Instant timestamp = reference.plusSeconds(i);
            observations.add(new Observation("integration-fixture-" + i, timestamp, source, "lab-user",
                    "FAILURE", "ubuntu-target", timestamp + entries.get(i).substring(entries.get(i).indexOf(' '))));
        }
        ingestion.ingest(session.id(), observations.subList(1, 5));
        assertTrue(workflow.list(exercise, null).isEmpty());
        ingestion.ingest(session.id(), observations.subList(0, 1));
        assertTrue(ingestion.ingest(session.id(), observations).stream().allMatch(SshObservationService.Receipt::duplicate));
        var alert = workflow.list(exercise, "OPEN").getFirst();
        assertEquals(5, workflow.detail(exercise, alert.id()).events().size());
        assertThrows(BusinessException.class, () -> workflow.command(exercise, alert.id(), actor, new Command("CLOSE", null, null, null)));
        String alertPath = "/api/v1/exercises/" + exercise + "/security-alerts/" + alert.id();
        mvc.perform(get(alertPath).with(blue)).andExpect(status().isOk());
        mvc.perform(get(alertPath).with(outsider)).andExpect(status().isForbidden());
        mvc.perform(post(alertPath + "/actions").with(blue).contentType("application/json")
                .content("{\"action\":\"ACKNOWLEDGE\",\"assessmentId\":" + assessment + "}")).andExpect(status().isOk());
        mvc.perform(post(alertPath + "/actions").with(blue).contentType("application/json")
                .content("""
                        {"action":"ANALYZE","detail":"Observed same-source authentication failures"}
                        """)).andExpect(status().isOk());
        mvc.perform(post(alertPath + "/actions").with(blue).contentType("application/json")
                .content("""
                        {"action":"REQUEST_CONTAINMENT","detail":"Request from student"}
                        """)).andExpect(status().isForbidden());
        workflow.command(exercise, alert.id(), actor, new Command("REQUEST_CONTAINMENT", "Limit the affected Lab client", null, null));
        assertEquals(alert.id(), workflow.nextContainment(session.id()).alertId());
        workflow.recordContainment(session.id(), alert.id(), new ProbeResult(true, false));
        assertEquals(alert.id(), workflow.nextContainment(session.id()).alertId());
        workflow.recordContainment(session.id(), alert.id(), new ProbeResult(true, true));
        assertNull(workflow.nextContainment(session.id()));
        mvc.perform(post(alertPath + "/actions").with(blue).contentType("application/json")
                .content("""
                        {"action":"SUBMIT_REPORT","detail":"Containment and SSH recovery verified","title":"SSH incident"}
                        """)).andExpect(status().isOk());
        workflow.command(exercise, alert.id(), actor, new Command("CLOSE", null, null, null));

        var request = new EvaluationRequest(true, "lab-user", "ubuntu-target", true);
        var finalResult = evaluator.evaluate(exercise, alert.id(), actor, request);
        assertEquals(new BigDecimal("100.00"), finalResult.finalScore());
        assertEquals("REVIEWED", finalResult.reviewStatus());
        assertEquals(finalResult.finalScore(), evaluator.evaluate(exercise, alert.id(), actor, request).finalScore());
        assertEquals(new BigDecimal("100.00"), assessments.getAssessment(assessment, Long.toString(actor), "ADMIN").finalScore());
        assertEquals(7L, db.sql("SELECT count(*) FROM evidence WHERE ssh_assessment_id = :assessment")
                .param("assessment", assessment).query(Long.class).single());
        assertEquals(7L, db.sql("""
                SELECT count(*) FROM assessment_score_evidence l JOIN evidence e ON e.id = l.evidence_id
                WHERE e.ssh_assessment_id = :assessment
                """).param("assessment", assessment).query(Long.class).single());
        var results = assessments.getAssessmentResults(assessment, Long.toString(actor), "ADMIN");
        long score = results.getFirst().scores().getFirst().id();
        mvc.perform(get("/api/v1/assessment-scores/{id}/evidence", score).with(blue)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/assessment-scores/{id}/evidence", score).with(outsider)).andExpect(status().isForbidden());
        assertTrue(results.stream().allMatch(result -> "COMPLETED".equals(result.status())));
        assertTrue(results.getFirst().detectionTimeSeconds() >= 7);
        assertNotNull(results.getFirst().responseTimeSeconds());
        assertFalse(lab.stop(exercise).ready());
        lab.heartbeat(session.id(), "STOPPED", new Health(false, false, false, false, false, false, null, null));
        assertEquals("STOPPED", lab.status(exercise).status());
    }

    private long id(String query) {
        return db.sql(query).query(Long.class).single();
    }
}
