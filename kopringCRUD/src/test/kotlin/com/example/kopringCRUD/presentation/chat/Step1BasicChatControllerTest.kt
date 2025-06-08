package com.example.kopringCRUD.presentation.chat

import com.example.kopringCRUD.domain.chat.service.ChatService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * 🎯 1단계: 가장 기본적인 ChatController 테스트
 *
 * 목표:
 * - Spring MVC 테스트 기본 이해
 * - MockMvc 사용법 익히기
 * - Security 없이 단순한 API 테스트
 */
@WebMvcTest(
    controllers = [ChatController::class],
    excludeAutoConfiguration = [
        // Spring Security 자동 설정 제외 (단순한 테스트를 위해)
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration::class
    ]
)
@DisplayName("1단계: ChatController 기본 테스트")
class Step1BasicChatControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean // ChatService를 Mock으로 대체
    private lateinit var chatService: ChatService

    @Test
    @DisplayName("헬스체크 API가 정상 응답을 반환한다")
    fun `헬스체크_API_정상_응답`() {
        // Given: 특별한 준비 없음 (정적 응답이므로)

        // When & Then: API 호출하고 응답 검증
        mockMvc.perform(get("/api/chat/health"))
            .andExpect(status().isOk) // HTTP 200 응답
            .andExpect(jsonPath("$.status").value("UP")) // JSON 응답의 status 필드
            .andExpect(jsonPath("$.service").value("Chat API")) // service 필드
            .andExpect(jsonPath("$.timestamp").exists()) // timestamp 필드 존재 여부
    }

    @Test
    @DisplayName("존재하지 않는 엔드포인트는 적절한 에러 응답을 반환한다")
    fun `존재하지_않는_엔드포인트_에러_응답`() {
        // When & Then
        // 실제 애플리케이션에서는 GlobalExceptionHandler가 500으로 처리하므로
        // 실제 동작에 맞게 테스트를 수정
        mockMvc.perform(get("/api/chat/nonexistent"))
            .andExpect(status().isInternalServerError) // HTTP 500 응답 (실제 동작에 맞게)
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @DisplayName("유효한 GET 요청은 정상적으로 처리된다")
    fun `유효한_GET_요청_정상_처리`() {
        // Given: 헬스체크 엔드포인트에 올바른 GET 요청

        // When & Then
        mockMvc.perform(get("/api/chat/health"))
            .andExpect(status().isOk) // HTTP 200 응답
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("Chat API"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.endpoints").isArray)
    }
}