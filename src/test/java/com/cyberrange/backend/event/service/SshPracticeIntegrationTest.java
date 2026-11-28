package com.cyberrange.backend.event.service;

import com.cyberrange.backend.assessment.service.AssessmentService;
import com.cyberrange.backend.lab.service.SshLabService;
import com.cyberrange.backend.lab.service.SshPracticeService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "ssh.practice.enabled=true")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:554(?:39|40)/cyber_range_test")
class SshPracticeIntegrationTest {

    @Autowired JdbcClient db;
    @Autowired AssessmentService assessments;
    @Autowired SshLabService labs;
    @Autowired SshPracticeService practice;
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void threeRolesPublishScopedAssessmentWithExpectedRecoveryScore(boolean recovered) throws Exception {
        long exercise = db.sql("""
                INSERT INTO exercises(scenario_id,cyber_range_id,name,status)
                SELECT scenario_id,cyber_range_id,'Practice Integration Transaction','CREATED'
                FROM exercises WHERE name='SSH Fixture Exercise' RETURNING id
                """).query(Long.class).single();
        long blueTeam = db.sql("SELECT id FROM teams WHERE name='SSH Fixture Blue'").query(Long.class).single();
        long instructor = db.sql("SELECT id FROM users WHERE username='ssh-fixture-admin'").query(Long.class).single();
        long role = db.sql("INSERT INTO roles(name) VALUES ('STUDENT') ON CONFLICT (name) DO UPDATE SET name=excluded.name RETURNING id").query(Long.class).single();
        long redTeam = db.sql("INSERT INTO teams(name) VALUES ('Practice Test RED') RETURNING id").query(Long.class).single();
        long red = learner(exercise, redTeam, role, "practice-red", "RED");
        long blue = learner(exercise, blueTeam, role, "practice-blue", "BLUE");
        long white = learner(exercise, blueTeam, role, "practice-white", "WHITE");
        long otherTeam = db.sql("INSERT INTO teams(name) VALUES ('Practice Test Other Blue') RETURNING id").query(Long.class).single();
        long otherBlue = learner(exercise, otherTeam, role, "practice-other-blue", "BLUE");
        var redUser = user(red, "STUDENT");
        var blueUser = user(blue, "STUDENT");
        var staff = user(instructor, "INSTRUCTOR");

        long assessment = assessments.initializeAssessments(exercise, blueTeam).getFirst().id();
        var lab = labs.start(exercise, assessment);
        labs.heartbeat(lab.id(), "RUNNING", new SshLabService.Health(true, true, true, true, true, true, "192.0.2.10", null));

        String approvals = "/api/v1/assessments/" + assessment + "/ssh-practice";
        mvc.perform(get(approvals + "/approval").param("redTeamId", Long.toString(redTeam)).with(redUser))
                .andExpect(status().isForbidden());
        mvc.perform(get(approvals + "/approval").param("redTeamId", Long.toString(redTeam)).with(staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(lab.id().toString()));
        String approval = "{\"redTeamId\":" + redTeam + "}";
        mvc.perform(post(approvals).with(redUser).contentType("application/json").content(approval)).andExpect(status().isForbidden());
        mvc.perform(post(approvals).with(staff).contentType("application/json")
                .content("{\"redTeamId\":" + redTeam + ",\"labSessionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post(approvals).with(staff).contentType("application/json").content(approval)).andExpect(status().isOk());
        UUID id = practice.forAssessment(assessment).getFirst().id();
        assertEquals(id, practice.approve(assessment, redTeam, instructor).id());

        mvc.perform(get("/api/v1/ssh-practice").with(blueUser)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].observations").isEmpty());
        submit(id, blueUser, "{\"action\":\"START_SCENARIO\"}", 403);
        submit(id, redUser, "{\"action\":\"START_SCENARIO\"}", 409);
        submit(id, redUser, "{\"action\":\"CHECK_CONNECTION\"}", 200);
        submit(id, redUser, "{\"action\":\"CHECK_CONNECTION\"}", 200);
        assertEquals(id, practice.claimConnection(lab.id()));
        assertNull(practice.claimConnection(lab.id()));
        practice.finishConnection(lab.id(), id, true);
        submit(id, redUser, "{\"action\":\"START_SCENARIO\"}", 200);
        submit(id, redUser, "{\"action\":\"START_SCENARIO\"}", 200);

        mvc.perform(get("/api/v1/ssh-practice").with(redUser)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].observations").isEmpty());
        mvc.perform(get("/api/v1/ssh-practice").with(blueUser)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].alert").value(true)).andExpect(jsonPath("$.data[0].observations.length()").value(6));
        submit(id, user(Long.MAX_VALUE, "STUDENT"), "{\"action\":\"SAVE_INVESTIGATION\"}", 403);
        submit(id, user(otherBlue, "STUDENT"), "{\"action\":\"ACKNOWLEDGE\"}", 403);
        submit(id, user(white, "STUDENT"), "{\"action\":\"ACKNOWLEDGE\"}", 403);
        mvc.perform(get("/api/v1/ssh-practice").with(user(otherBlue, "STUDENT"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/v1/ssh-practice").with(user(white, "STUDENT"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].role").value("WHITE"));
        submit(id, blueUser, "{\"action\":\"SAVE_INVESTIGATION\"}", 409);
        submit(id, blueUser, "{\"action\":\"ACKNOWLEDGE\"}", 200);
        submit(id, blueUser, "{\"action\":\"ACKNOWLEDGE\"}", 200);
        submit(id, blueUser, """
                {"action":"SAVE_INVESTIGATION","observationIds":[1,2,4,5,6],"account":"lab-user",
                 "target":"ubuntu-target","source":"192.0.2.42","impact":"Tài khoản thử nghiệm",
                 "note":"Nhiều lỗi xác thực liên tiếp từ cùng một nguồn."}
                """, 200);
        submit(id, blueUser, """
                {"action":"PROPOSE_RESPONSE","note":"Đề xuất giới hạn truy cập từ nguồn đã xác minh."}
                """, 200);
        submit(id, blueUser, "{\"action\":\"APPROVE_RESPONSE\",\"note\":\"Không có quyền\"}", 403);
        submit(id, staff, "{\"action\":\"APPROVE_RESPONSE\",\"note\":\"Đề xuất phù hợp với log của lượt.\"}", 200);
        submit(id, blueUser, """
                {"action":"SAVE_REPORT_DRAFT","recovered":true,"reportSections":{"summary":"Đã bắt đầu viết."}}
                """, 200);
        submit(id, blueUser, """
                {"action":"SUBMIT_REPORT","recovered":true,"reportSections":{"summary":"Thiếu các mục bắt buộc."}}
                """, 400);
        submit(id, blueUser, """
                {"action":"SUBMIT_REPORT","recovered":%s,"reportSections":{
                 "summary":"Nhiều lỗi xác thực","timeline":"Các mốc từ 0 đến 30 giây.",
                 "relatedEvents":"1,2,4,5,6","affectedAccount":"lab-user","affectedHost":"ubuntu-target",
                 "findings":"Các lỗi có cùng nguồn.","proposedResponse":"Giới hạn nguồn đã xác minh.",
                 "responseOutcome":"Đã duyệt trong mô phỏng.","recoveryVerification":"Xác thực hợp lệ thành công.",
                 "lessonsLearned":"Kiểm tra lại dịch vụ sau phản ứng."}}
                """.formatted(recovered), 200);
        submit(id, staff, """
                {"action":"REVIEW","note":"Kết luận phù hợp với dữ liệu.","analysisAccepted":true,"reportAccepted":true}
                """, 200);

        mvc.perform(get("/api/v1/ssh-practice").with(blueUser)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].score").value(recovered ? 100 : 85));
        assertEquals("REVIEWED", practice.forAssessment(assessment).getFirst().state());
        assertEquals(0L, db.sql("SELECT count(*) FROM events WHERE exercise_id=:id").param("id", exercise).query(Long.class).single());
        submit(id, staff, "{\"action\":\"REVIEW\"}", 200);
        assertEquals(7L, db.sql("""
                SELECT count(*) FROM evidence WHERE exercise_id=:id AND data->>'origin'='TRAINING_SIMULATION'
                """).param("id", exercise).query(Long.class).single());
        assertEquals(recovered ? 100 : 85,
                assessments.getAssessment(assessment, Long.toString(instructor), "ADMIN").finalScore().intValueExact());
        assertNotNull(practice.forAssessment(assessment).getFirst().metrics().studentResponseSeconds());
    }

    private long learner(long exercise, long team, long role, String username, String assignment) {
        long user = db.sql("""
                INSERT INTO users(role_id,username,email,password_hash,full_name)
                VALUES (:role,:name,:email,'test-login-disabled',:name) RETURNING id
                """).param("role", role).param("name", username).param("email", username + "@example.invalid")
                .query(Long.class).single();
        db.sql("INSERT INTO exercise_participants(exercise_id,user_id,team_id,role) VALUES (:exercise,:user,:team,:role)")
                .param("exercise", exercise).param("user", user).param("team", team).param("role", assignment).update();
        return user;
    }

    private JwtRequestPostProcessor user(long id, String role) {
        return jwt().jwt(token -> token.subject(Long.toString(id)).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private void submit(UUID id, JwtRequestPostProcessor actor, String json, int code) throws Exception {
        mvc.perform(post("/api/v1/ssh-practice/{id}/actions", id).with(actor)
                .contentType("application/json").content(json)).andExpect(status().is(code));
    }
}
