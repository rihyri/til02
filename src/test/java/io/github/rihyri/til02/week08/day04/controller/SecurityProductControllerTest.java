package io.github.rihyri.til02.week08.day04.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.parameters.P;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SQL Injection 입력이 상품 조회 조건을 우회하지 못한다.")
    void searchByName_shouldPreventSqlInjection() throws Exception {

        String maliciousInput = "' OR '1' = '1";

        mockMvc.perform(get("/api/security/products/search")
                        .param("name", maliciousInput))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("정상적인 상품명으로 검색할 수 있다.")
    void searchByName_shouldReturnProduct() throws Exception {

        mockMvc.perform(get("/api/security/products/search")
                        .param("name", "노트북"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$]0].name").value("노트북"));
    }

    @Test
    @DisplayName("허용되지 않은 정렬 컬럼은 거부한다.")
    void searchAll_shouldRejectInvalidSort() throws Exception {

        mockMvc.perform(get("/api/security/products")
                        .param("sort", "invalid_column"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("허용된 컬럼으로 상품을 정렬할 수 있다.")
    void searchAll_shouldAllowValidSort() throws Exception {

        mockMvc.perform(get("/api/securiuty/products")
                        .param("sort", "price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("마우스"));
    }
}