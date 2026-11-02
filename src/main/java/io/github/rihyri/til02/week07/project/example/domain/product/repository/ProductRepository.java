package io.github.rihyri.til02.week07.project.example.domain.product.repository;

import io.github.rihyri.til02.week07.project.example.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByExternalProductIdIn(Collection<String> externalProducts);
}
