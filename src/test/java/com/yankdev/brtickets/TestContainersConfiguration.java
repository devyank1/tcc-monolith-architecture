package com.yankdev.brtickets;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    @Bean
    @ServiceConnection
    public org.testcontainers.postgresql.PostgreSQLContainer postgreSQLContainer() {
        return new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine");
    }

}
