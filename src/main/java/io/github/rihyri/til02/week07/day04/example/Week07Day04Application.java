package io.github.rihyri.til02.week07.day04.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableRetry    // @Retryable을 사용한 메서드 재시도를 활성화한다.
@EnableAsync    // 비동기 메서드를 사용할 수 있도록 설정한다.
@EnableFeignClients // @FeignClient로 선언된 인터페이스를 HTTP 클라이언트로 등록한다.
@SpringBootApplication
public class Week07Day04Application {

    public static void main (String[] args) {
        SpringApplication.run(Week07Day04Application.class, args);
    }
}
