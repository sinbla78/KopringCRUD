package com.example.kopringCRUD.presentation.websocket

import com.example.kopringCRUD.domain.chat.dto.ChatMessageResponse
import com.example.kopringCRUD.domain.chat.entity.MessageType
import com.example.kopringCRUD.domain.user.dto.UserResponse
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket 이벤트 데이터 클래스들
 */
data class UserJoinedEvent(
    val roomId: Long,
    val user: UserResponse,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class UserLeftEvent(
    val roomId: Long,
    val user: UserResponse,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class SystemMessageEvent(
    val roomId: Long,
    val message: String,
    val messageType: MessageType = MessageType.SYSTEM,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class TypingEvent(
    val roomId: Long,
    val user: UserResponse,
    val isTyping: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class OnlineStatusEvent(
    val userId: Long,
    val username: String,
    val isOnline: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class NotificationEvent(
    val userId: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val data: Map<String, Any>? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class NotificationType {
    CHAT_MESSAGE,     // 새 채팅 메시지
    MENTION,          // 멘션 알림
    REPORT_HANDLED,   // 신고 처리 완료
    SYSTEM_NOTICE,    // 시스템 공지
    WARNING           // 경고 메시지
}

/**
 * WebSocket 이벤트 처리 및 실시간 알림 관리
 */
@Component
class WebSocketEventHandler(
    private val messagingTemplate: SimpMessagingTemplate
) {

    // 온라인 사용자 관리 (SessionId -> UserId)
    private val onlineUsers = ConcurrentHashMap<String, Long>()

    // 채팅방별 참여자 관리 (RoomId -> Set<UserId>)
    private val roomParticipants = ConcurrentHashMap<Long, MutableSet<Long>>()

    // 사용자별 활성 세션 관리 (UserId -> Set<SessionId>)
    private val userSessions = ConcurrentHashMap<Long, MutableSet<String>>()

    // ========================================
    // WebSocket 연결/해제 이벤트 처리
    // ========================================

    /**
     * WebSocket 연결 이벤트 처리
     */
    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectedEvent) {
        val headers = StompHeaderAccessor.wrap(event.message)
        val sessionId = headers.sessionId ?: return

        println("🔗 WebSocket 연결 성공: sessionId=$sessionId")

        // 사용자 정보 추출 (실제로는 JWT 토큰에서 추출)
        val userId = extractUserIdFromHeaders(headers)
        if (userId != null) {
            // 온라인 사용자 등록
            onlineUsers[sessionId] = userId

            // 사용자별 세션 관리
            userSessions.computeIfAbsent(userId) { mutableSetOf() }.add(sessionId)

            // 온라인 상태 알림
            broadcastOnlineStatus(userId, true)

            println("👤 사용자 온라인: userId=$userId, sessionId=$sessionId")
        }
    }

    /**
     * WebSocket 연결 해제 이벤트 처리
     */
    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headers = StompHeaderAccessor.wrap(event.message)
        val sessionId = headers.sessionId ?: return

        println("🔌 WebSocket 연결 해제: sessionId=$sessionId")

        // 사용자 정보 조회
        val userId = onlineUsers.remove(sessionId)
        if (userId != null) {
            // 사용자 세션에서 제거
            userSessions[userId]?.remove(sessionId)

            // 모든 세션이 종료되면 오프라인 처리
            if (userSessions[userId]?.isEmpty() == true) {
                userSessions.remove(userId)
                broadcastOnlineStatus(userId, false)

                // 참여 중인 모든 채팅방에서 퇴장 처리
                roomParticipants.forEach { (roomId, participants) ->
                    if (participants.remove(userId)) {
                        // 퇴장 알림은 선택적으로 처리 (너무 많은 알림 방지)
                        println("👋 사용자 채팅방 퇴장: userId=$userId, roomId=$roomId")
                    }
                }
            }

            println("👤 사용자 오프라인: userId=$userId")
        }
    }

    // ========================================
    // 채팅방 관련 이벤트 처리
    // ========================================

    /**
     * 사용자 채팅방 입장 이벤트 처리
     */
    fun handleUserJoined(event: UserJoinedEvent) {
        // 채팅방 참여자 등록
        roomParticipants.computeIfAbsent(event.roomId) { mutableSetOf() }.add(event.user.id)

        // 입장 시스템 메시지 생성
        val systemMessage = ChatMessageResponse(
            id = 0,
            roomId = event.roomId,
            sender = event.user,
            content = "${event.user.username}님이 채팅방에 입장했습니다.",
            messageType = MessageType.SYSTEM,
            isDeleted = false,
            createdAt = event.timestamp,
            updatedAt = event.timestamp
        )

        // 채팅방 구독자들에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/${event.roomId}", systemMessage)

        // 참여자 목록 업데이트 알림
        broadcastRoomParticipants(event.roomId)

        println("🚪 채팅방 입장: userId=${event.user.id}, roomId=${event.roomId}")
    }

    /**
     * 사용자 채팅방 퇴장 이벤트 처리
     */
    fun handleUserLeft(event: UserLeftEvent) {
        // 채팅방 참여자에서 제거
        roomParticipants[event.roomId]?.remove(event.user.id)

        // 퇴장 시스템 메시지 생성
        val systemMessage = ChatMessageResponse(
            id = 0,
            roomId = event.roomId,
            sender = event.user,
            content = "${event.user.username}님이 채팅방에서 나갔습니다.",
            messageType = MessageType.SYSTEM,
            isDeleted = false,
            createdAt = event.timestamp,
            updatedAt = event.timestamp
        )

        // 채팅방 구독자들에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/${event.roomId}", systemMessage)

        // 참여자 목록 업데이트 알림
        broadcastRoomParticipants(event.roomId)

        println("🚪 채팅방 퇴장: userId=${event.user.id}, roomId=${event.roomId}")
    }

    /**
     * 시스템 메시지 이벤트 처리
     */
    fun handleSystemMessage(event: SystemMessageEvent) {
        val systemMessage = ChatMessageResponse(
            id = 0,
            roomId = event.roomId,
            sender = createSystemUser(), // 시스템 사용자
            content = event.message,
            messageType = event.messageType,
            isDeleted = false,
            createdAt = event.timestamp,
            updatedAt = event.timestamp
        )

        messagingTemplate.convertAndSend("/topic/room/${event.roomId}", systemMessage)

        println("📢 시스템 메시지: roomId=${event.roomId}, message=${event.message}")
    }

    // ========================================
    // 타이핑 상태 관리
    // ========================================

    /**
     * 타이핑 상태 이벤트 처리
     */
    fun handleTypingEvent(event: TypingEvent) {
        // 타이핑 상태를 해당 채팅방의 다른 사용자들에게 알림
        messagingTemplate.convertAndSend("/topic/room/${event.roomId}/typing", mapOf(
            "userId" to event.user.id,
            "username" to event.user.username,
            "isTyping" to event.isTyping,
            "timestamp" to event.timestamp
        ))
    }

    // ========================================
    // 개인 알림 처리
    // ========================================

    /**
     * 개인 알림 전송
     */
    fun sendPersonalNotification(event: NotificationEvent) {
        // 특정 사용자에게만 알림 전송
        messagingTemplate.convertAndSendToUser(
            event.userId.toString(),
            "/queue/notifications",
            mapOf(
                "title" to event.title,
                "message" to event.message,
                "type" to event.type.name,
                "data" to event.data,
                "timestamp" to event.timestamp
            )
        )

        println("🔔 개인 알림 전송: userId=${event.userId}, type=${event.type}")
    }

    /**
     * 멘션 알림 처리
     */
    fun handleMentionNotification(mentionedUserId: Long, message: ChatMessageResponse) {
        val notification = NotificationEvent(
            userId = mentionedUserId,
            title = "새로운 멘션",
            message = "${message.sender.username}님이 회원님을 언급했습니다: ${message.content}",
            type = NotificationType.MENTION,
            data = mapOf(
                "roomId" to message.roomId,
                "messageId" to message.id,
                "senderId" to message.sender.id
            )
        )

        sendPersonalNotification(notification)
    }

    /**
     * 신고 처리 완료 알림
     */
    fun handleReportHandledNotification(reporterId: Long, reportId: Long, status: String) {
        val notification = NotificationEvent(
            userId = reporterId,
            title = "신고 처리 완료",
            message = "접수하신 신고가 처리되었습니다. 상태: $status",
            type = NotificationType.REPORT_HANDLED,
            data = mapOf("reportId" to reportId)
        )

        sendPersonalNotification(notification)
    }

    // ========================================
    // 온라인 상태 관리
    // ========================================

    /**
     * 온라인 상태 브로드캐스트
     */
    private fun broadcastOnlineStatus(userId: Long, isOnline: Boolean) {
        val statusEvent = OnlineStatusEvent(
            userId = userId,
            username = getUsernameById(userId) ?: "Unknown",
            isOnline = isOnline
        )

        // 전체 사용자에게 온라인 상태 알림
        messagingTemplate.convertAndSend("/topic/online-status", statusEvent)
    }

    /**
     * 채팅방 참여자 목록 브로드캐스트
     */
    private fun broadcastRoomParticipants(roomId: Long) {
        val participants = roomParticipants[roomId] ?: emptySet()
        val participantInfo = participants.map { userId ->
            mapOf(
                "userId" to userId,
                "username" to getUsernameById(userId),
                "isOnline" to isUserOnline(userId)
            )
        }

        messagingTemplate.convertAndSend("/topic/room/$roomId/participants", mapOf(
            "roomId" to roomId,
            "participants" to participantInfo,
            "count" to participants.size
        ))
    }

    // ========================================
    // 유틸리티 메소드들
    // ========================================

    /**
     * 헤더에서 사용자 ID 추출 (JWT 토큰 파싱)
     */
    private fun extractUserIdFromHeaders(headers: StompHeaderAccessor): Long? {
        // 실제 구현에서는 JWT 토큰을 파싱하여 사용자 ID 추출
        // 현재는 테스트용으로 간단한 구현
        return try {
            val token = headers.getFirstNativeHeader("Authorization")
            // JWT 파싱 로직 구현 필요
            // jwtTokenProvider.getUserIdFromToken(token)
            1L // 임시값
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 사용자 ID로 사용자명 조회
     */
    private fun getUsernameById(userId: Long): String? {
        // 실제 구현에서는 UserService 또는 캐시에서 조회
        return "user$userId" // 임시 구현
    }

    /**
     * 사용자 온라인 상태 확인
     */
    private fun isUserOnline(userId: Long): Boolean {
        return userSessions[userId]?.isNotEmpty() == true
    }

    /**
     * 시스템 사용자 정보 생성
     */
    private fun createSystemUser(): UserResponse {
        return UserResponse(
            id = 0,
            username = "System",
            email = "system@example.com",
            displayName = "시스템",
            avatarUrl = null,
            role = com.example.kopringCRUD.domain.user.entity.UserRole.ADMIN,
            isActive = true,
            createdAt = LocalDateTime.now()
        )
    }

    // ========================================
    // 공개 API (다른 컴포넌트에서 호출)
    // ========================================

    /**
     * 특정 채팅방에 시스템 메시지 전송
     */
    fun sendSystemMessageToRoom(roomId: Long, message: String) {
        val event = SystemMessageEvent(roomId, message)
        handleSystemMessage(event)
    }

    /**
     * 전체 공지사항 브로드캐스트
     */
    fun broadcastSystemNotice(title: String, message: String) {
        val announcement = mapOf(
            "title" to title,
            "message" to message,
            "type" to "SYSTEM_NOTICE",
            "timestamp" to LocalDateTime.now()
        )

        messagingTemplate.convertAndSend("/topic/announcements", announcement)

        println("📢 전체 공지사항: $title")
    }

    /**
     * 온라인 사용자 수 조회
     */
    fun getOnlineUserCount(): Int {
        return userSessions.size
    }

    /**
     * 채팅방별 참여자 수 조회
     */
    fun getRoomParticipantCount(roomId: Long): Int {
        return roomParticipants[roomId]?.size ?: 0
    }

    /**
     * 사용자별 활성 세션 수 조회
     */
    fun getUserSessionCount(userId: Long): Int {
        return userSessions[userId]?.size ?: 0
    }

    /**
     * 특정 사용자에게 경고 메시지 전송
     */
    fun sendWarningToUser(userId: Long, reason: String) {
        val notification = NotificationEvent(
            userId = userId,
            title = "경고",
            message = "이용 규칙 위반으로 인한 경고입니다. 사유: $reason",
            type = NotificationType.WARNING,
            data = mapOf("reason" to reason)
        )

        sendPersonalNotification(notification)
    }

    /**
     * 채팅방 강제 퇴장 처리
     */
    fun forceLeaveRoom(userId: Long, roomId: Long, reason: String) {
        // 참여자 목록에서 제거
        roomParticipants[roomId]?.remove(userId)

        // 강제 퇴장 메시지 전송
        sendSystemMessageToRoom(roomId, "사용자가 관리자에 의해 퇴장되었습니다. 사유: $reason")

        // 해당 사용자에게 개인 알림
        sendPersonalNotification(NotificationEvent(
            userId = userId,
            title = "채팅방에서 퇴장되었습니다",
            message = "관리자에 의해 채팅방에서 퇴장되었습니다. 사유: $reason",
            type = NotificationType.WARNING,
            data = mapOf("roomId" to roomId, "reason" to reason)
        ))

        // 클라이언트에게 강제 퇴장 신호 전송
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/force-leave",
            mapOf("roomId" to roomId, "reason" to reason)
        )
    }
}