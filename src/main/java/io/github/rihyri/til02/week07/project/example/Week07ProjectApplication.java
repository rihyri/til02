package io.github.rihyri.til02.week07.project.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableRetry
@EnableFeignClients
@SpringBootApplication
public class Week07ProjectApplication {

    public static void main (String[] args) {
        SpringApplication.run(Week07ProjectApplication.class, args);
    }
}
