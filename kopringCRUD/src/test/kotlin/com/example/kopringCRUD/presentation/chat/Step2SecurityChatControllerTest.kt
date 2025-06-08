package com.example.kopringCRUD.presentation.chat

import com.example.kopringCRUD.domain.chat.service.ChatService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * 🎯 2단계: Spring Security와 함께하는 테스트
 *
 * 목표:
 * - 인증이 필요한 API 테스트 방법 학습
 * - @WithMockUser 사용법 익히기
 * - 권한별 접근 제어 테스트
 * - 401, 403 에러 처리 테스트
 */
@WebMvcTest(ChatController::class) // Security 포함 (excludeAutoConfiguration 제거)
@DisplayName("2단계: Spring Security 포함 테스트")
class Step2SecurityChatControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var chatService: ChatService

    @Test
    @WithAnonymousUser // 인증되지 않은 사용자
    @DisplayName("인증되지 않은 사용자는 401 Unauthorized를 받는다")
    fun `인증되지_않은_사용자_401_에러`() {
        // When & Then
        mockMvc.perform(get("/api/chat/health"))
            .andExpect(status().isUnauthorized) // HTTP 401
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @DisplayName("인증된 일반 사용자는 헬스체크에 접근할 수 있다")
    fun `인증된_일반사용자_헬스체크_성공`() {
        // When & Then
        mockMvc.perform(get("/api/chat/health"))
            .andExpect(status().isOk) // HTTP 200
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("Chat API"))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    @DisplayName("관리자는 헬스체크에 접근할 수 있다")
    fun `관리자_헬스체크_성공`() {
        // When & Then
        mockMvc.perform(get("/api/chat/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    @WithMockUser(username = "moderator", roles = ["MODERATOR"])
    @DisplayName("모더레이터는 헬스체크에 접근할 수 있다")
    fun `모더레이터_헬스체크_성공`() {
        // When & Then
        mockMvc.perform(get("/api/chat/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.service").value("Chat API"))
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @DisplayName("일반 사용자도 채팅방 목록을 조회할 수 있다")
    fun `일반사용자_채팅방목록_조회_성공`() {
        // Given: MockBean으로 ChatService가 자동으로 Mock됨
        // 실제 서비스 호출은 하지 않고 컨트롤러 로직만 테스트

        // When & Then
        mockMvc.perform(get("/api/chat/rooms"))
            .andExpect(status().isOk) // 기본적으로 200 응답 기대
    }
}