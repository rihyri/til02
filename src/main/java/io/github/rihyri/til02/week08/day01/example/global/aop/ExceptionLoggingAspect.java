package io.github.rihyri.til02.week08.day01.example.global.aop;

import io.github.rihyri.til02.week08.day01.example.global.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExceptionLoggingAspect {

    // Service 계층의 모든 메서드를 대상으로 지정한다.
    @Pointcut("execution(* io.github.rihyri.til02.week08.day01.example.domain.service.*.*(..))")
    private void serviceMethods() {
    }

    // 대상 메서드에서 ProductNotFoundException이 전파되면 실행된다.
    @AfterThrowing(
            pointcut = "serviceMethods()",
            throwing = "exception"
    )
    public void logServiceException(JoinPoint joinPoint, ProductNotFoundException exception) {

        // 예외가 발생한 메서드와 예외 정보를 출력한다.
        log.error(
                "[Exception] Method: {}, code: {}, Message: {}",
                joinPoint.getSignature().getName(),
                exception.getCode(),
                exception.getMessage()
        );
    }
}
