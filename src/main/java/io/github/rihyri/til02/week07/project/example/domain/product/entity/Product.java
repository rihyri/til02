package io.github.rihyri.til02.week07.project.example.domain.product.entity;

import io.github.rihyri.til02.week07.project.example.domain.category.entity.Category;
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

    // 외부 상품 ID
    @Column(unique = true, length = 50)
    String externalProductId;

    // 상품 위치 출처
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ProductSource source;

    @Column(nullable = false)
    Boolean isOrderable;

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
        this.isOrderable = calcOrderable(stock);
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
            this.isOrderable = calcOrderable(stock);
        }
    }

    public static Product fromExternal(String externalId, Category category, String name,
                                       String description, BigDecimal price, Integer stock) {

        Product p = new Product(category, name, description, price, stock);
        p.externalProductId = externalId;
        p.source = ProductSource.EXTERNAL;
        return p;
    }

    // 외부 동기화 전용
    public void syncFromExternal(String name, String description, BigDecimal price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.isOrderable = calcOrderable(stock);
    }

    // 재고 개산
    private static boolean calcOrderable(Integer stock) {
        return stock != null && stock > 0;
    }
}