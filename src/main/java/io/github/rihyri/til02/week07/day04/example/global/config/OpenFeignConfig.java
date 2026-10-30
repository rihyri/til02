package io.github.rihyri.til02.week07.day04.example.global.config;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OpenFeignConfig {

    @Bean
    public Request.Options feignOptions() {

        return new Request.Options(
                // 외부 서버 연결을 최대 10초 기다림
                10000, TimeUnit.MILLISECONDS,

                // 연결 후 응답을 최대 60초 기다림
                60000, TimeUnit.MILLISECONDS,

                // HTTP 리다이렉트 허용
                true
        );
    }

    @Bean
    public Retryer feignRetryer() {

        // Feign 자체 재시도를 비활성화
        return Retryer.NEVER_RETRY;

        // Feign 재시도를 사용하는 경우 비교 코드
        // return new Retryer.Default(100, 1000, 3);
    }
}
