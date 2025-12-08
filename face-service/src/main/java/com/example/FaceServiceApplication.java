package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = {JacksonAutoConfiguration.class}, scanBasePackages = {"com.example", "com.example.config"})
@EnableDiscoveryClient
@MapperScan("com.example.mapper")
public class FaceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaceServiceApplication.class, args);
    }
}