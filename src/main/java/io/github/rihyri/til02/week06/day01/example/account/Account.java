package io.github.rihyri.til02.week06.day01.example.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Account {

    private final Long id;
    private final String owner;
    private final long balance;
}
