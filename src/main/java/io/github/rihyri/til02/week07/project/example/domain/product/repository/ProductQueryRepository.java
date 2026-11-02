package io.github.rihyri.til02.week07.project.example.domain.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.rihyri.til02.week07.project.example.domain.category.entity.QCategory;
import io.github.rihyri.til02.week07.project.example.domain.product.dto.ProductSearchResponse;
import io.github.rihyri.til02.week07.project.example.domain.product.entity.QProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<ProductSearchResponse> search(String categoryName, Boolean isOrderable, Pageable pageable) {

        QProduct product = QProduct.product;
        QCategory category = QCategory.category;

        // 1) 조건 만들기: 값이 있는 것만 and로 붙임
        BooleanBuilder where = new BooleanBuilder();

        if (StringUtils.hasText(categoryName)) {
            where.and(category.name.eq(categoryName));
        }

        if (isOrderable != null) {
            where.and(product.isOrderable.eq(isOrderable));
        }

        // 2) 목록 조회 (SELECT ... FROM product JOIN category WHERE ... LIMIT ...)
        List<ProductSearchResponse> content = queryFactory
                .select(Projections.constructor(ProductSearchResponse.class,
                        product.id,
                        product.name,
                        product.price,
                        product.stock,
                        product.isOrderable,
                        category.name))
                .from(product)
                .join(product.category, category)
                .where(where)
                .orderBy(product.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 3) 전체 개수 조회 (SELECT COUNT(*) ...)
        Long total = queryFactory
                .select(product.count())
                .from(product)
                .join(product.category, category)
                .where(where)
                .fetchOne();

        // 목록 + 전체 개수를 Page로 묶기
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
