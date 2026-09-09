package br.com.bitabit.ecclesiacontrol;

import br.com.bitabit.ecclesiacontrol.core.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

class BackendApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Passa se o contexto Spring subir com sucesso E o schema
        // bater com o Flyway (ddl-auto: validate)
    }
}