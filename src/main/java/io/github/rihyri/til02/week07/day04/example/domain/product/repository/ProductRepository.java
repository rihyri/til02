package io.github.rihyri.til02.week07.day04.example.domain.product.repository;

import io.github.rihyri.til02.week07.day04.example.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
