package com.example.compressiontool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.compressiontool")
@EnableJpaRepositories("com.example.compressiontool")
public class CompressionToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompressionToolApplication.class, args);
    }
}
