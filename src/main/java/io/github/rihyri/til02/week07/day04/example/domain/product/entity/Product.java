package io.github.rihyri.til02.week07.day04.example.domain.product.entity;

import io.github.rihyri.til02.week07.day04.example.domain.category.entity.Category;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
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

    // 상품이 소속된 카테고리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Category category;

    String name;

    String description;

    BigDecimal price;

    Integer stock;

    // 낙관적 락을 위한 버전 필드
    @Version
    Integer version;

    @Builder
    public Product(
            Category category,
            String name,
            String description,
            BigDecimal price,
            Integer stock
    ) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    // 전달된 카테고리가 존재할 때만 변경
    public void setCategory(Category category) {
        if (!ObjectUtils.isEmpty(category)) {
            this.category = category;
        }
    }

    // 빈 문자열이 아닐 때만 상품명 변경
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

    // 주문 수량만큼 재고 감소
    public void reduceStock(Integer quantity) {
        this.stock -= quantity;
    }

    // 취소 또는 반품 수량만큼 재고 증가
    public void increaseStock(Integer quantity) {
        this.stock += quantity;
    }
}