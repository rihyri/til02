package io.github.rihyri.til02.week08.day01.example.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    //  1. execution()
    //  api 패키지에 있는 메서드 실행 전에 로그를 출력한다.
    @Before("execution(* io.github.rihyri.til02.week08.day01.example.domain.api.*.*(..))")
    public void logBeforeApi() {
        log.info("[execution] API 메서드 실행 전");
    }

    //  2. within()
    //  domain 패키지와 하위 패키지의 모든 메서드를 적용한다.
    @Before("within(io.github.rihyri.til02.week08.day01.example.domain..*)")
    public void logBeforeDomain() {
        log.info("[within] domain 메서드 실행 전");
    }

    //  3. @annotation()
    //  @Loggable 어노테이션이 붙은 메서드에만 실행한다.
    //  @Loggable이 없다고 해서 다른 execution()이나 within()로그까지 출력되지 않는건 아니다.
    @Before("@annotation(io.github.rihyri.til02.week08.day01.example.global.annotation.Loggable)")
    public void logBeforeAnnotation() {
        log.info("[annotation] @Loggable 메서드 실행 전");
    }

    //  4. JoinPoint
    //  실행되는 메서드 이름과 전달된 인자를 확인한다.
    @Before("execution(* io.github.rihyri.til02.week08.day01.example.domain..*.*(..))")
    public void logMethodDetails(JoinPoint joinPoint) {

        // 실행되는 메서드 이름을 가져온다.
        String methodName = joinPoint.getSignature().getName();

        log.info("[JoinPoint] 메서드 이름: {}", methodName);

        // 메서드에 전달된 인자를 가져온다.
        Object[] args = joinPoint.getArgs();

        if (args.length > 0) {
            for (Object arg : args) {
                log.info("[JoinPoint] 전달된 인자: {}", args);
            }
        }
    }
}
