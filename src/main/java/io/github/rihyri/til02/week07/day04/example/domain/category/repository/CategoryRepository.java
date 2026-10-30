package io.github.rihyri.til02.week07.day04.example.domain.category.repository;

import io.github.rihyri.til02.week07.day04.example.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
