package ca.dtadmi.gamehubapi;

import ca.dtadmi.gamehubapi.config.FirebaseConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "ca.dtadmi.gamehubapi")
@EntityScan("ca.dtadmi.gamehubapi")
@EnableJpaRepositories("ca.dtadmi.gamehubapi")
@EnableCaching
@EnableConfigurationProperties(FirebaseConfig.class)
public class GamehubApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(GamehubApiApplication.class, args);
    }
}
