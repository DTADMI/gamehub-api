package ca.dtadmi.gamehubapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "ca.dtadmi.gamehubapi")
@EntityScan("ca.dtadmi.gamehubapi.model")
@EnableJpaRepositories("ca.dtadmi.gamehubapi.repository")
@EnableCaching
public class GamehubApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(GamehubApiApplication.class, args);
    }
}
