package io.github.rihyri.til02.week06.project.product.repository;

import io.github.rihyri.til02.week06.project.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
