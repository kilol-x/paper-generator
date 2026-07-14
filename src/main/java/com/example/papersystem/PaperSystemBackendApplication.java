package com.example.papersystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.papersystem.mapper")
public class PaperSystemBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaperSystemBackendApplication.class, args);
    }
}