package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 *  애그리거트 루트의 공통 기능: 도메인 이벤트를 모아 두었다가 응용 계층이 꺼내 발행한다.
 *  도메인은 "무슨 일이 일어났는지"만 기록하고, "어떻게 전달할지(Spring, Kafka)"는 모른다.
 */
@MappedSuperclass
public abstract class AggregateRoot {

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    protected void registerEvent(Object event) {
        domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }
}
