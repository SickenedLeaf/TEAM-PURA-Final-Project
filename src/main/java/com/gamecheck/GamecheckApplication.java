package com.gamecheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GamecheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamecheckApplication.class, args);
    }
}
