package br.com.bitabit.ecclesiacontrol;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Test passes if Spring context loads successfully
        // JPA will auto-create tables from entities using ddl-auto: create-drop
    }
}