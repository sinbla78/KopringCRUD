package com.example.kopringCRUD.presentation.chat

import com.example.kopringCRUD.domain.chat.dto.*
import com.example.kopringCRUD.domain.chat.entity.MessageType
import com.example.kopringCRUD.domain.chat.service.ChatService
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import com.example.kopringCRUD.global.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.security.Principal
import org.springframework.security.core.Authentication
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

/**
 * 채팅 관리 REST API 컨트롤러
 * 채팅방 생성/관리, 메시지 송수신 기능 제공
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"]) // 개발 환경용
class ChatController(
    private val chatService: ChatService
) {

    // ========================================
    // 채팅방 관리 API
    // ========================================

    /**
     * 새로운 채팅방 생성
     *
     * @param userPrincipal 현재 인증된 사용자 (채팅방 생성자)
     * @param request 채팅방 생성 정보
     * @return 생성된 채팅방 정보
     */
    @PostMapping("/rooms")
    fun createRoom(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: CreateChatRoomRequest
    ): ResponseEntity<ChatRoomResponse> {
        val room = chatService.createRoom(userPrincipal.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(room)
    }

    /**
     * 활성 채팅방 목록 조회 (페이징)
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param search 검색 키워드 (채팅방 이름, 선택적)
     * @return 채팅방 목록
     */
    @GetMapping("/rooms")
    fun getRooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<PageResponse<ChatRoomResponse>> {
        val pageRequest = PageRequest(page, size)

        val rooms = if (search != null) {
            chatService.searchRoomsByName(search, pageRequest)
        } else {
            chatService.findActiveRooms(pageRequest)
        }

        return ResponseEntity.ok(rooms)
    }

    /**
     * 특정 채팅방 정보 조회
     *
     * @param roomId 조회할 채팅방 ID
     * @return 채팅방 정보
     */
    @GetMapping("/rooms/{roomId}")
    fun getRoom(@PathVariable roomId: Long): ResponseEntity<ChatRoomResponse> {
        val room = chatService.findRoomById(roomId)
        return ResponseEntity.ok(room)
    }

    /**
     * 채팅방 정보 수정 (생성자만 가능)
     *
     * @param userPrincipal 현재 인증된 사용자
     * @param roomId 수정할 채팅방 ID
     * @param request 수정할 정보
     * @return 수정된 채팅방 정보
     */
    @PutMapping("/rooms/{roomId}")
    fun updateRoom(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: UpdateChatRoomRequest
    ): ResponseEntity<ChatRoomResponse> {
        val updatedRoom = chatService.updateRoom(roomId, userPrincipal.id, request)
        return ResponseEntity.ok(updatedRoom)
    }

    /**
     * 채팅방 비활성화 (생성자 또는 관리자)
     *
     * @param userPrincipal 현재 인증된 사용자
     * @param roomId 비활성화할 채팅방 ID
     * @return 비활성화된 채팅방 정보
     */
    @DeleteMapping("/rooms/{roomId}")
    fun deactivateRoom(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable roomId: Long
    ): ResponseEntity<ChatRoomResponse> {
        val deactivatedRoom = chatService.deactivateRoom(
            roomId,
            userPrincipal.id,
            getUserRole(userPrincipal)
        )
        return ResponseEntity.ok(deactivatedRoom)
    }

    /**
     * 내가 생성한 채팅방 목록 조회
     *
     * @param userPrincipal 현재 인증된 사용자
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 내가 생성한 채팅방 목록
     */
    @GetMapping("/rooms/my")
    fun getMyRooms(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<ChatRoomResponse>> {
        val pageRequest = PageRequest(page, size)
        val rooms = chatService.findMyRooms(userPrincipal.id, pageRequest)
        return ResponseEntity.ok(rooms)
    }

    // ========================================
    // 메시지 관리 API
    // ========================================

    /**
     * 메시지 전송
     *
     * @param userPrincipal 현재 인증된 사용자 (메시지 발송자)
     * @param roomId 메시지를 보낼 채팅방 ID
     * @param request 메시지 내용
     * @return 전송된 메시지 정보
     */
    @PostMapping("/rooms/{roomId}/messages")
    fun sendMessage(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: SendMessageRequest
    ): ResponseEntity<ChatMessageResponse> {
        val message = chatService.sendMessage(userPrincipal.id, roomId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(message)
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이징)
     *
     * @param roomId 조회할 채팅방 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param search 검색 키워드 (메시지 내용, 선택적)
     * @return 메시지 목록
     */
    @GetMapping("/rooms/{roomId}/messages")
    fun getMessages(
        @PathVariable roomId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<PageResponse<ChatMessageResponse>> {
        val pageRequest = PageRequest(page, size)

        val messages = if (search != null) {
            chatService.searchMessages(roomId, search, pageRequest)
        } else {
            chatService.findMessagesByRoom(roomId, pageRequest)
        }

        return ResponseEntity.ok(messages)
    }

    /**
     * 특정 메시지 조회
     *
     * @param messageId 조회할 메시지 ID
     * @return 메시지 정보
     */
    @GetMapping("/messages/{messageId}")
    fun getMessage(@PathVariable messageId: Long): ResponseEntity<ChatMessageResponse> {
        val message = chatService.findMessageById(messageId)
        return ResponseEntity.ok(message)
    }

    /**
     * 메시지 삭제 (작성자, 모더레이터, 관리자만 가능)
     *
     * @param userPrincipal 현재 인증된 사용자
     * @param messageId 삭제할 메시지 ID
     * @return 삭제된 메시지 정보
     */
    @DeleteMapping("/messages/{messageId}")
    fun deleteMessage(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable messageId: Long
    ): ResponseEntity<ChatMessageResponse> {
        val deletedMessage = chatService.deleteMessage(
            messageId,
            userPrincipal.id,
            getUserRole(userPrincipal)
        )
        return ResponseEntity.ok(deletedMessage)
    }

    /**
     * 특정 채팅방의 최근 메시지 조회 (채팅방 입장 시 사용)
     *
     * @param roomId 조회할 채팅방 ID
     * @param limit 조회할 메시지 개수 (기본값: 50)
     * @return 최근 메시지 목록
     */
    @GetMapping("/rooms/{roomId}/messages/recent")
    fun getRecentMessages(
        @PathVariable roomId: Long,
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<ChatMessageResponse>> {
        val messages = chatService.findRecentMessages(roomId, limit)
        return ResponseEntity.ok(messages)
    }

    // ========================================
    // 관리자 전용 기능
    // ========================================

    /**
     * 모든 채팅방 조회 (관리자 전용)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param search 검색 키워드
     * @return 모든 채팅방 목록 (비활성 포함)
     */
    @GetMapping("/admin/rooms")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getAllRooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<PageResponse<ChatRoomResponse>> {
        val pageRequest = PageRequest(page, size)
        val rooms = chatService.findAllRooms(pageRequest)
        return ResponseEntity.ok(rooms)
    }

    /**
     * 채팅방 강제 활성화 (관리자 전용)
     *
     * @param roomId 활성화할 채팅방 ID
     * @return 활성화된 채팅방 정보
     */
    @PutMapping("/admin/rooms/{roomId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    fun activateRoom(@PathVariable roomId: Long): ResponseEntity<ChatRoomResponse> {
        val activatedRoom = chatService.activateRoom(roomId)
        return ResponseEntity.ok(activatedRoom)
    }

    /**
     * 메시지 복구 (관리자 전용)
     *
     * @param messageId 복구할 메시지 ID
     * @return 복구된 메시지 정보
     */
    @PutMapping("/admin/messages/{messageId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    fun restoreMessage(@PathVariable messageId: Long): ResponseEntity<ChatMessageResponse> {
        val restoredMessage = chatService.restoreMessage(messageId)
        return ResponseEntity.ok(restoredMessage)
    }

    /**
     * 채팅방 통계 조회 (관리자/모더레이터)
     *
     * @param roomId 통계를 조회할 채팅방 ID
     * @return 채팅방 통계 정보
     */
    @GetMapping("/admin/rooms/{roomId}/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getRoomStatistics(@PathVariable roomId: Long): ResponseEntity<Map<String, Any>> {
        val statistics = chatService.getRoomStatistics(roomId)
        return ResponseEntity.ok(statistics)
    }

    /**
     * 전체 채팅 통계 조회 (관리자 전용)
     *
     * @return 전체 채팅 시스템 통계
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    fun getOverallStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = chatService.getOverallStatistics()
        return ResponseEntity.ok(statistics)
    }

    // ========================================
    // 파일 업로드 관련 (향후 확장)
    // ========================================

    /**
     * 파일 업로드 (이미지, 파일 메시지용)
     * 실제 구현에서는 별도의 파일 서비스나 클라우드 스토리지 사용 권장
     *
     * @param file 업로드할 파일
     * @return 업로드된 파일 정보
     */
    @PostMapping("/upload")
    fun uploadFile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam("file") file: org.springframework.web.multipart.MultipartFile
    ): ResponseEntity<FileUploadResponse> {
        // 실제 구현에서는 파일 저장 로직 구현 필요
        // AWS S3, Google Cloud Storage, 로컬 파일 시스템 등 활용

        val fileUploadResponse = FileUploadResponse(
            fileUrl = "https://example.com/uploads/${file.originalFilename}",
            fileName = file.originalFilename ?: "unknown",
            fileSize = file.size,
            contentType = file.contentType ?: "application/octet-stream"
        )

        return ResponseEntity.ok(fileUploadResponse)
    }

    // ========================================
    // 유틸리티 메소드들
    // ========================================

    /**
     * UserPrincipal에서 UserRole 추출
     */
    private fun getUserRole(userPrincipal: UserPrincipal): com.example.kopringCRUD.domain.user.entity.UserRole {
        // UserPrincipal의 authorities에서 역할 추출
        val roleAuthority = userPrincipal.authorities.find { it.authority.startsWith("ROLE_") }
        val roleName = roleAuthority?.authority?.removePrefix("ROLE_") ?: "USER"

        return try {
            com.example.kopringCRUD.domain.user.entity.UserRole.valueOf(roleName)
        } catch (e: IllegalArgumentException) {
            com.example.kopringCRUD.domain.user.entity.UserRole.USER
        }
    }

    /**
     * API 상태 확인 (헬스체크)
     *
     * @return API 상태 정보
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "Chat API",
                "timestamp" to System.currentTimeMillis(),
                "endpoints" to listOf(
                    mapOf("method" to "POST", "path" to "/api/chat/rooms", "description" to "채팅방 생성"),
                    mapOf("method" to "GET", "path" to "/api/chat/rooms", "description" to "채팅방 목록 조회"),
                    mapOf("method" to "GET", "path" to "/api/chat/rooms/{roomId}", "description" to "채팅방 정보 조회"),
                    mapOf("method" to "POST", "path" to "/api/chat/rooms/{roomId}/messages", "description" to "메시지 전송"),
                    mapOf("method" to "GET", "path" to "/api/chat/rooms/{roomId}/messages", "description" to "메시지 목록 조회"),
                    mapOf("method" to "DELETE", "path" to "/api/chat/messages/{messageId}", "description" to "메시지 삭제"),
                    mapOf("method" to "GET", "path" to "/api/chat/rooms/my", "description" to "내가 만든 채팅방"),
                    mapOf("method" to "POST", "path" to "/api/chat/upload", "description" to "파일 업로드")
                )
            )
        )
    }
}

/**
 * WebSocket을 통한 실시간 채팅 컨트롤러
 * STOMP 프로토콜 사용
 */
@Controller
class WebSocketChatController(
    private val chatService: ChatService
) {

    /**
     * WebSocket으로 메시지 전송
     * 클라이언트에서 /app/chat/{roomId}로 메시지 전송
     */
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/room/{roomId}")
    fun sendMessage(
        @DestinationVariable roomId: Long,
        message: SendMessageRequest,
        principal: Principal
    ): ChatMessageResponse {
        val userPrincipal = extractUserPrincipal(principal)
        return chatService.sendMessage(userPrincipal.id, roomId, message)
    }

    /**
     * 채팅방 입장 알림 - 단순화된 버전
     * 클라이언트에서 /app/chat/{roomId}/join으로 입장 알림
     */
    @MessageMapping("/chat/{roomId}/join")
    @SendTo("/topic/room/{roomId}")
    fun joinRoom(
        @DestinationVariable roomId: Long
    ): ChatMessageResponse {
        // 임시로 시스템 메시지 생성 (실제로는 인증된 사용자 정보 필요)
        val joinMessage = "새로운 사용자가 채팅방에 입장했습니다."
        return chatService.sendSystemMessage(roomId, joinMessage)
    }

    /**
     * 채팅방 퇴장 알림 - 단순화된 버전
     * 클라이언트에서 /app/chat/{roomId}/leave로 퇴장 알림
     */
    @MessageMapping("/chat/{roomId}/leave")
    @SendTo("/topic/room/{roomId}")
    fun leaveRoom(
        @DestinationVariable roomId: Long
    ): ChatMessageResponse {
        // 임시로 시스템 메시지 생성 (실제로는 인증된 사용자 정보 필요)
        val leaveMessage = "사용자가 채팅방에서 나갔습니다."
        return chatService.sendSystemMessage(roomId, leaveMessage)
    }

    /**
     * Principal에서 UserPrincipal 추출하는 헬퍼 메소드
     */
    private fun extractUserPrincipal(principal: Principal): UserPrincipal {
        return when (principal) {
            is Authentication -> {
                principal.principal as? UserPrincipal
                    ?: throw IllegalStateException("인증된 사용자가 아닙니다.")
            }
            else -> {
                // Principal에서 직접 사용자명 추출하여 UserPrincipal 생성
                // 실제 구현에서는 사용자 서비스에서 사용자 정보를 조회해야 함
                throw IllegalStateException("지원하지 않는 Principal 타입입니다: ${principal::class.java}")
            }
        }
    }
}