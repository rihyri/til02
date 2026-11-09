package io.github.rihyri.til02.week08.day04.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String USER_JSON = """
            {
                "name": "홍길동",
                "email": "test@example.com",
                "password": "password123"
            }
            """;

    @Test
    @DisplayName("CSRF 토큰이 없으면 POST 요청은 거부된다.")
    void postWithoutCsrf_shouldBeForbidden() throws Exception {

        mockMvc.perform(post("/api/security/users/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF 토큰이 있으면 정상 요청이 처리된다.")
    void postWithCsrf_shouldSucceed() throws Exception {

        mockMvc.perform(post("/api/security/users/validate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(USER_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효한 CSRF 토큰이 있어도 잘못된 입력값은 거부된다.")
    void invalidUserRequest_shouldBeRejected() throws Exception {

        String invalidJson = """
                {
                    "name": "홍길동",
                    "email": "invalid-email",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/api/security/users/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("허용된 Origin의 Preflight 요청은 통과한다.")
    void cors_shouldAllowTrustedOrigin() throws Exception {

        mockMvc.perform(options("/api/security/users/validate")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:3000"
                ));
    }

    @Test
    @DisplayName("허용되지 않은 Origin의 Preflight 요청은 거부한다")
    void cors_shouldRejectUntrustedOrigin() throws Exception {

        mockMvc.perform(options("/api/security/users/validate")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}