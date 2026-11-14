package com.cyberrange.backend.dashboard.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countTotalUsers() {
        String sql = """
                SELECT COUNT(*)
                FROM users
                """;

        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    public long countActiveExercises() {
        String sql = """
                SELECT COUNT(*)
                FROM exercises
                WHERE status = 'RUNNING'
                """;

        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    public long countCompletedAssessments() {
        String sql = """
                SELECT COUNT(*)
                FROM assessments
                WHERE status = 'COMPLETED'
                """;

        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
}
