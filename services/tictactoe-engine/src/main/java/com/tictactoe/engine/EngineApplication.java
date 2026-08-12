package com.tictactoe.engine;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Game Engine Service",
        description = "Owns Tic Tac Toe board state: validates moves and determines game outcome.",
        version = "v1"
))
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }
}