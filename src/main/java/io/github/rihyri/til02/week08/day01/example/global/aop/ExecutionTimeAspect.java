package io.github.rihyri.til02.week08.day01.example.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {

    // Service 패키지의 모든 메서드를 대상으로 설정한다.
    @Pointcut("execution(* io.github.rihyri.til02.week08.day01.example.domain.service.*.*(..))")
    private void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1. 실제 메서드 실행 전 시작 시간을 기록한다.
        long startTime = System.currentTimeMillis();

        try {
            // 2. 원래 실행하던 서비스 메서드를 호출한다.
            Object result = joinPoint.proceed();

            // 3. 원래 메서드의 결과를 그대로 반환한다.
            return result;

        } finally {
            // 4. 정상 종료 또는 예외 발생 후 실행 시간을 계산한다.
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info(
                    "[ExecutionTime] {} 실행 시간: {}ms",
                    joinPoint.getSignature().toShortString(),
                    executionTime
            );
        }
    }
}
