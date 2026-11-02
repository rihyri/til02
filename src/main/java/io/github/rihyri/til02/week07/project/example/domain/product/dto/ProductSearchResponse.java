package io.github.rihyri.til02.week07.project.example.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ProductSearchResponse {

    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final Integer stock;

    @JsonProperty("is_orderable")
    private final Boolean isOrderable;

    private final String categoryName;

    public ProductSearchResponse (Long id, String name, BigDecimal price,
                                  Integer stock, Boolean isOrderable, String categoryName) {

        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.isOrderable = isOrderable;
        this.categoryName = categoryName;
    }
}
