package io.github.rihyri.til02.week07.day03.example.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest (long userId, List<ProductRequest> products) {
    public record ProductRequest (String name, BigDecimal price, int quantity) { }
}
