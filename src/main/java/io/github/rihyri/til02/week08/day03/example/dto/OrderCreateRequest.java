package io.github.rihyri.til02.week08.day03.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    private Long userId;
    private List<OrderProductRequest> product;
}
