package io.github.rihyri.til02.week07.day04.example.domain.category.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Table(name = "categories")
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Category parentCategory;

    @Builder
    public Category(
            String name,
            Category parentCategory) {
        this.name = name;
        this.parentCategory = parentCategory;
    }

    public void setName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
    }

    public void setParentCategory(Category parentCategory) {
        if (!ObjectUtils.isEmpty(parentCategory)) {
            this.parentCategory = parentCategory;
        }
    }
}