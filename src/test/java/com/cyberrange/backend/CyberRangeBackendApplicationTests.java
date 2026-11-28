package com.cyberrange.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:554(?:39|40)/cyber_range_test")
class CyberRangeBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
