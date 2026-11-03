package io.github.rihyri.til02.week08.day01.example.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)     // 메서드에 붙일 수 있도록 설정한다.
@Retention(RetentionPolicy.RUNTIME) //  실행 중에도 어노테이션 정보를 확인할 수 있도록 한다.
public @interface Loggable {    // 직접 만든 커스텀 어노테이션
}
