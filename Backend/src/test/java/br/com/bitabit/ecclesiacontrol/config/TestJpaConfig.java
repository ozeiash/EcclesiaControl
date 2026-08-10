package br.com.bitabit.ecclesiacontrol.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

@TestConfiguration
public class TestJpaConfig {
    // Opcional: customize teste JPA aqui se necessário
}