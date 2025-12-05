package ca.dtadmi.gamehubapi;

import org.springframework.boot.SpringApplication;

public class TestGamehubApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(GamehubApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
