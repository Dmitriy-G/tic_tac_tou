package com.tictactoe.gamesession;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Game Session Service",
        description = "Manages game sessions and automates moves via the Game Engine Service.",
        version = "v1"
))
public class GameSessionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameSessionServiceApplication.class, args);
    }
}