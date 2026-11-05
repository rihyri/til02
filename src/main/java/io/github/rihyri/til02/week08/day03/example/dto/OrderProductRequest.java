package io.github.rihyri.til02.week08.day03.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductRequest {

    private Long productId;
    private int quantity;
}
