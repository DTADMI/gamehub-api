package ca.dtadmi.gamehubapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
/*
    @Bean(initMethod = "start", destroyMethod = "stop")
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"))
            .withExposedPorts(9092, 9093);
    }*/

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ServiceConnection
        // Provides spring.datasource.* to the context
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
    }
/*
    @Bean(initMethod = "start", destroyMethod = "stop")
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.11-management"))
            .withExposedPorts(5672, 15672);
    }*/

}
