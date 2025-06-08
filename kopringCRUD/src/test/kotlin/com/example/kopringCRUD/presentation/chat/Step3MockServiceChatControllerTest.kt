package com.example.kopringCRUD.presentation.chat

import com.example.kopringCRUD.domain.chat.dto.ChatRoomResponse
import com.example.kopringCRUD.domain.chat.dto.CreateChatRoomRequest
import com.example.kopringCRUD.domain.chat.service.ChatService
import com.example.kopringCRUD.domain.user.dto.UserResponse
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import com.example.kopringCRUD.global.security.UserPrincipal
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * 🎯 3단계: Mock을 사용한 서비스 테스트
 *
 * 목표:
 * - Mockito 사용법 학습
 * - 서비스 메소드 Mock하기
 * - 컨트롤러와 서비스 간의 상호작용 테스트
 * - JSON 요청/응답 테스트
 */
@WebMvcTest(ChatController::class)
@DisplayName("3단계: Mock 서비스 테스트")
class Step3MockServiceChatControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // 테스트용 Mock UserPrincipal 생성
    private fun createMockUserPrincipal(id: Long = 1L, username: String = "testuser"): UserPrincipal {
        return mock(UserPrincipal::class.java).apply {
            `when`(this.id).thenReturn(id)
            `when`(this.username).thenReturn(username)
        }
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @DisplayName("채팅방 목록 조회가 정상 작동한다")
    fun `채팅방_목록_조회_성공`() {
        // Given: Mock 데이터 준비
        val mockChatRooms = listOf(
            ChatRoomResponse(
                id = 1L,
                name = "테스트 채팅방 1",
                description = "첫 번째 테스트 채팅방",
                maxParticipants = 100,
                creator = UserResponse(
                    id = 1L,
                    username = "user1",
                    email = "user1@test.com",
                    displayName = "User One",
                    avatarUrl = null,
                    role = UserRole.USER,
                    isActive = true,
                    createdAt = LocalDateTime.now()
                ),
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            ChatRoomResponse(
                id = 2L,
                name = "테스트 채팅방 2",
                description = "두 번째 테스트 채팅방",
                maxParticipants = 50,
                creator = UserResponse(
                    id = 2L,
                    username = "user2",
                    email = "user2@test.com",
                    displayName = "User Two",
                    avatarUrl = null,
                    role = UserRole.USER,
                    isActive = true,
                    createdAt = LocalDateTime.now()
                ),
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val pageResponse = PageResponse(
            content = mockChatRooms,
            totalElements = 2L,
            totalPages = 1,
            currentPage = 0,
            size = 20,
            hasNext = false,
            hasPrevious = false
        )

        // ✅ 안전한 방법: 구체적인 PageRequest 객체 생성
        val expectedPageRequest = PageRequest(page = 0, size = 20)

        // Stubbing - 구체적인 객체 사용
        `when`(chatService.findActiveRooms(expectedPageRequest))
            .thenReturn(pageResponse)

        // When & Then: API 호출 및 검증
        mockMvc.perform(
            get("/api/chat/rooms")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("테스트 채팅방 1"))
            .andExpect(jsonPath("$.content[1].name").value("테스트 채팅방 2"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.currentPage").value(0))

        // Verify - 구체적인 객체로 검증
        verify(chatService).findActiveRooms(expectedPageRequest)
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @DisplayName("채팅방 생성이 정상 작동한다")
    fun `채팅방_생성_성공`() {
        // Given: 요청 데이터와 Mock 응답 준비
        val createRequest = CreateChatRoomRequest(
            name = "새로운 채팅방",
            description = "새로 만든 채팅방입니다",
            maxParticipants = 20
        )

        val mockResponse = ChatRoomResponse(
            id = 3L,
            name = "새로운 채팅방",
            description = "새로 만든 채팅방입니다",
            maxParticipants = 20,
            creator = UserResponse(
                id = 1L,
                username = "testuser",
                email = "testuser@test.com",
                displayName = "Test User",
                avatarUrl = null,
                role = UserRole.USER,
                isActive = true,
                createdAt = LocalDateTime.now()
            ),
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // ✅ 안전한 방법: 구체적인 값들로 stubbing
        `when`(chatService.createRoom(1L, createRequest))
            .thenReturn(mockResponse)

        // UserPrincipal Mock 생성
        val userPrincipal = createMockUserPrincipal()

        // When & Then: API 호출 및 검증
        mockMvc.perform(
            post("/api/chat/rooms")
                .with(user(userPrincipal))
                .with(csrf()) // ✅ CSRF 토큰 추가
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated) // HTTP 201
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.name").value("새로운 채팅방"))
            .andExpect(jsonPath("$.description").value("새로 만든 채팅방입니다"))
            .andExpect(jsonPath("$.creator.username").value("testuser"))
            .andExpect(jsonPath("$.isActive").value(true))

        // Verify - 구체적인 값들로 검증
        verify(chatService).createRoom(1L, createRequest)
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @DisplayName("특정 채팅방 조회가 정상 작동한다")
    fun `특정_채팅방_조회_성공`() {
        // Given: Mock 응답 준비
        val roomId = 1L
        val mockResponse = ChatRoomResponse(
            id = roomId,
            name = "조회용 채팅방",
            description = "조회 테스트용 채팅방",
            maxParticipants = 50,
            creator = UserResponse(
                id = 1L,
                username = "creator",
                email = "creator@test.com",
                displayName = "Creator User",
                avatarUrl = null,
                role = UserRole.USER,
                isActive = true,
                createdAt = LocalDateTime.now()
            ),
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // ✅ 안전한 방법: 구체적인 값으로 stubbing
        `when`(chatService.findRoomById(roomId))
            .thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/$roomId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(roomId))
            .andExpect(jsonPath("$.name").value("조회용 채팅방"))
            .andExpect(jsonPath("$.maxParticipants").value(50))

        // Verify - 구체적인 값으로 검증
        verify(chatService).findRoomById(roomId)
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @DisplayName("검색 키워드로 채팅방을 찾을 수 있다")
    fun `채팅방_검색_성공`() {
        // Given: 검색 결과 Mock 데이터
        val searchKeyword = "게임"
        val searchResults = listOf(
            ChatRoomResponse(
                id = 1L,
                name = "게임 채팅방",
                description = "게임 관련 채팅방",
                maxParticipants = 30,
                creator = UserResponse(
                    id = 1L,
                    username = "gamer",
                    email = "gamer@test.com",
                    displayName = "Gamer User",
                    avatarUrl = null,
                    role = UserRole.USER,
                    isActive = true,
                    createdAt = LocalDateTime.now()
                ),
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val pageResponse = PageResponse(
            content = searchResults,
            totalElements = 1L,
            totalPages = 1,
            currentPage = 0,
            size = 20,
            hasNext = false,
            hasPrevious = false
        )

        // ✅ 안전한 방법: 구체적인 값들로 stubbing
        val expectedPageRequest = PageRequest(page = 0, size = 20)
        `when`(chatService.searchRoomsByName(searchKeyword, expectedPageRequest))
            .thenReturn(pageResponse)

        // When & Then
        mockMvc.perform(
            get("/api/chat/rooms")
                .param("search", searchKeyword)
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("게임 채팅방"))
            .andExpect(jsonPath("$.totalElements").value(1))

        // Verify - 구체적인 값들로 검증
        verify(chatService).searchRoomsByName(searchKeyword, expectedPageRequest)
    }
}