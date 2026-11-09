package io.github.rihyri.til02.week08.day04.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SecurityProductResponse {

    private Long id;
    private String name;
    private Integer price;
}
