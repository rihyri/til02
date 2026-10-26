package io.github.rihyri.til02.week06.project.product.entity;

import io.github.rihyri.til02.week06.project.category.entity.Category;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Table
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Category category;

    String name;

    String description;

    BigDecimal price;

    Integer stock;

    @Builder
    public Product(
            Category category,
            String name,
            String description,
            BigDecimal price,
            Integer stock) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public void setCategory(Category category) {
        if (!ObjectUtils.isEmpty(category)) {
            this.category = category;
        }
    }

    public void setName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
    }

    public void setDescription(String description) {
        if (StringUtils.hasText(description)) {
            this.description = description;
        }
    }

    public void setPrice(BigDecimal price) {
        if (!ObjectUtils.isEmpty(price)) {
            this.price = price;
        }
    }

    public void setStock(Integer stock) {
        if (!ObjectUtils.isEmpty(stock)) {
            this.stock = stock;
        }
    }
}