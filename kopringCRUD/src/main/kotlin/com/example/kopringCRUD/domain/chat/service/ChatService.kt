package com.example.kopringCRUD.domain.chat.service

import com.example.kopringCRUD.domain.chat.dto.*
import com.example.kopringCRUD.domain.chat.entity.ChatMessage
import com.example.kopringCRUD.domain.chat.entity.ChatRoom
import com.example.kopringCRUD.domain.chat.entity.ChatRoomInfo
import com.example.kopringCRUD.domain.chat.entity.MessageContent
import com.example.kopringCRUD.domain.chat.entity.MessageType
import com.example.kopringCRUD.domain.chat.repository.ChatMessageRepository
import com.example.kopringCRUD.domain.chat.repository.ChatRoomRepository
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.domain.user.repository.UserRepository
import com.example.kopringCRUD.domain.user.service.toResponse
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import com.example.kopringCRUD.global.exception.EntityNotFoundException
import com.example.kopringCRUD.global.exception.ForbiddenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository
) {

    // ========================================
    // 채팅방 관련 기능
    // ========================================

    /**
     * 새로운 채팅방 생성
     */
    fun createRoom(creatorId: Long, request: CreateChatRoomRequest): ChatRoomResponse {
        val creator = userRepository.findById(creatorId)
            .orElseThrow { EntityNotFoundException("User", creatorId) }

        val chatRoom = ChatRoom(
            roomInfo = ChatRoomInfo(
                name = request.name,
                description = request.description,
                maxParticipants = request.maxParticipants
            ),
            creator = creator
        )

        val savedRoom = chatRoomRepository.save(chatRoom)
        return savedRoom.toResponse()
    }

    /**
     * 채팅방 정보 수정 (생성자만 가능)
     */
    fun updateRoom(roomId: Long, userId: Long, request: UpdateChatRoomRequest): ChatRoomResponse {
        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { EntityNotFoundException("ChatRoom", roomId) }

        // 생성자 권한 확인
        if (!room.isCreatedBy(userId)) {
            throw ForbiddenException("채팅방 생성자만 수정할 수 있습니다")
        }

        val updatedInfo = room.roomInfo.copy(
            name = request.name ?: room.roomInfo.name,
            description = request.description ?: room.roomInfo.description,
            maxParticipants = request.maxParticipants ?: room.roomInfo.maxParticipants
        )

        val updatedRoom = chatRoomRepository.save(room.updateInfo(updatedInfo))
        return updatedRoom.toResponse()
    }

    /**
     * 채팅방 ID로 조회
     */
    @Transactional(readOnly = true)
    fun findRoomById(id: Long): ChatRoomResponse {
        val room = chatRoomRepository.findById(id)
            .orElseThrow { EntityNotFoundException("ChatRoom", id) }
        return room.toResponse()
    }

    /**
     * 활성 채팅방 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun findActiveRooms(pageRequest: PageRequest): PageResponse<ChatRoomResponse> {
        val rooms = chatRoomRepository.findActiveRooms(pageRequest)
        return PageResponse(
            content = rooms.content.map { it.toResponse() },
            totalElements = rooms.totalElements,
            totalPages = rooms.totalPages,
            currentPage = rooms.currentPage,
            size = rooms.size,
            hasNext = rooms.hasNext,
            hasPrevious = rooms.hasPrevious
        )
    }

    /**
     * 모든 채팅방 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    fun findAllRooms(pageRequest: PageRequest): PageResponse<ChatRoomResponse> {
        val rooms = chatRoomRepository.findAllRooms(pageRequest)
        return PageResponse(
            content = rooms.content.map { it.toResponse() },
            totalElements = rooms.totalElements,
            totalPages = rooms.totalPages,
            currentPage = rooms.currentPage,
            size = rooms.size,
            hasNext = rooms.hasNext,
            hasPrevious = rooms.hasPrevious
        )
    }

    /**
     * 채팅방 이름으로 검색
     */
    @Transactional(readOnly = true)
    fun searchRoomsByName(name: String, pageRequest: PageRequest): PageResponse<ChatRoomResponse> {
        val rooms = chatRoomRepository.findByNameContaining(name, pageRequest)
        return PageResponse(
            content = rooms.content.map { it.toResponse() },
            totalElements = rooms.totalElements,
            totalPages = rooms.totalPages,
            currentPage = rooms.currentPage,
            size = rooms.size,
            hasNext = rooms.hasNext,
            hasPrevious = rooms.hasPrevious
        )
    }

    /**
     * 내가 생성한 채팅방 조회
     */
    @Transactional(readOnly = true)
    fun findMyRooms(creatorId: Long, pageRequest: PageRequest): PageResponse<ChatRoomResponse> {
        val rooms = chatRoomRepository.findByCreatorId(creatorId, pageRequest)
        return PageResponse(
            content = rooms.content.map { it.toResponse() },
            totalElements = rooms.totalElements,
            totalPages = rooms.totalPages,
            currentPage = rooms.currentPage,
            size = rooms.size,
            hasNext = rooms.hasNext,
            hasPrevious = rooms.hasPrevious
        )
    }

    /**
     * 채팅방 비활성화 (생성자 또는 관리자)
     */
    fun deactivateRoom(roomId: Long, userId: Long, userRole: UserRole): ChatRoomResponse {
        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { EntityNotFoundException("ChatRoom", roomId) }

        // 권한 확인
        if (!room.isCreatedBy(userId) && userRole != UserRole.ADMIN && userRole != UserRole.MODERATOR) {
            throw ForbiddenException("채팅방을 비활성화할 권한이 없습니다")
        }

        val deactivatedRoom = chatRoomRepository.save(room.deactivate())
        return deactivatedRoom.toResponse()
    }

    /**
     * 채팅방 활성화 (관리자만)
     */
    fun activateRoom(roomId: Long): ChatRoomResponse {
        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { EntityNotFoundException("ChatRoom", roomId) }

        val activatedRoom = chatRoomRepository.save(room.activate())
        return activatedRoom.toResponse()
    }

    // ========================================
    // 메시지 관련 기능
    // ========================================

    /**
     * 메시지 전송
     */
    fun sendMessage(senderId: Long, roomId: Long, request: SendMessageRequest): ChatMessageResponse {
        val sender = userRepository.findById(senderId)
            .orElseThrow { EntityNotFoundException("User", senderId) }

        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { EntityNotFoundException("ChatRoom", roomId) }

        // 채팅방 참여 가능 여부 확인
        if (!room.canJoin()) {
            throw ForbiddenException("비활성화된 채팅방에는 메시지를 보낼 수 없습니다")
        }

        val message = ChatMessage(
            room = room,
            sender = sender,
            messageContent = MessageContent(
                content = request.content,
                messageType = request.messageType,
                fileUrl = request.fileUrl,
                fileName = request.fileName,
                fileSize = request.fileSize
            )
        )

        val savedMessage = chatMessageRepository.save(message)
        return savedMessage.toResponse()
    }

    /**
     * 시스템 메시지 전송 (입장/퇴장 알림 등)
     */
    fun sendSystemMessage(roomId: Long, content: String): ChatMessageResponse {
        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { EntityNotFoundException("ChatRoom", roomId) }

        // 시스템 메시지는 채팅방 생성자를 sender로 사용
        val message = ChatMessage(
            room = room,
            sender = room.creator,
            messageContent = MessageContent(
                content = content,
                messageType = MessageType.SYSTEM
            )
        )

        val savedMessage = chatMessageRepository.save(message)
        return savedMessage.toResponse()
    }

    /**
     * 특정 채팅방의 메시지 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun findMessagesByRoom(roomId: Long, pageRequest: PageRequest): PageResponse<ChatMessageResponse> {
        // 채팅방 존재 확인
        if (!chatRoomRepository.existsById(roomId)) {
            throw EntityNotFoundException("ChatRoom", roomId)
        }

        val messages = chatMessageRepository.findActiveMessagesByRoomId(roomId, pageRequest)
        return PageResponse(
            content = messages.content.map { it.toResponse() },
            totalElements = messages.totalElements,
            totalPages = messages.totalPages,
            currentPage = messages.currentPage,
            size = messages.size,
            hasNext = messages.hasNext,
            hasPrevious = messages.hasPrevious
        )
    }

    /**
     * 메시지 ID로 조회
     */
    @Transactional(readOnly = true)
    fun findMessageById(id: Long): ChatMessageResponse {
        val message = chatMessageRepository.findById(id)
            .orElseThrow { EntityNotFoundException("ChatMessage", id) }
        return message.toResponse()
    }

    /**
     * 메시지 삭제 (작성자, 모더레이터, 관리자만 가능)
     */
    fun deleteMessage(messageId: Long, userId: Long, userRole: UserRole): ChatMessageResponse {
        val message = chatMessageRepository.findById(messageId)
            .orElseThrow { EntityNotFoundException("ChatMessage", messageId) }

        // 삭제 권한 확인
        if (!message.canBeDeletedBy(userId, userRole)) {
            throw ForbiddenException("메시지를 삭제할 권한이 없습니다")
        }

        val deletedMessage = chatMessageRepository.save(message.delete())
        return deletedMessage.toResponse()
    }

    /**
     * 메시지 복구 (관리자만)
     */
    fun restoreMessage(messageId: Long): ChatMessageResponse {
        val message = chatMessageRepository.findById(messageId)
            .orElseThrow { EntityNotFoundException("ChatMessage", messageId) }

        val restoredMessage = chatMessageRepository.save(message.restore())
        return restoredMessage.toResponse()
    }

    /**
     * 채팅방의 최근 메시지 조회
     */
    @Transactional(readOnly = true)
    fun findRecentMessages(roomId: Long, limit: Int = 50): List<ChatMessageResponse> {
        if (!chatRoomRepository.existsById(roomId)) {
            throw EntityNotFoundException("ChatRoom", roomId)
        }

        val messages = chatMessageRepository.findRecentMessagesByRoomId(roomId, limit)
        return messages.map { it.toResponse() }
    }

    /**
     * 메시지 검색
     */
    @Transactional(readOnly = true)
    fun searchMessages(roomId: Long, keyword: String, pageRequest: PageRequest): PageResponse<ChatMessageResponse> {
        if (!chatRoomRepository.existsById(roomId)) {
            throw EntityNotFoundException("ChatRoom", roomId)
        }

        val messages = chatMessageRepository.findByRoomIdAndContentContaining(roomId, keyword, pageRequest)
        return PageResponse(
            content = messages.content.map { it.toResponse() },
            totalElements = messages.totalElements,
            totalPages = messages.totalPages,
            currentPage = messages.currentPage,
            size = messages.size,
            hasNext = messages.hasNext,
            hasPrevious = messages.hasPrevious
        )
    }

    // ========================================
    // 통계 및 유틸리티 기능
    // ========================================

    /**
     * 채팅방 통계 조회
     */
    @Transactional(readOnly = true)
    fun getRoomStatistics(roomId: Long): Map<String, Any> {
        if (!chatRoomRepository.existsById(roomId)) {
            throw EntityNotFoundException("ChatRoom", roomId)
        }

        return mapOf(
            "totalMessages" to chatMessageRepository.countByRoomId(roomId),
            "activeMessages" to chatMessageRepository.countActiveMessagesByRoomId(roomId),
            "deletedMessages" to (chatMessageRepository.countByRoomId(roomId) - chatMessageRepository.countActiveMessagesByRoomId(roomId))
        )
    }

    /**
     * 전체 채팅 통계 (관리자용)
     */
    @Transactional(readOnly = true)
    fun getOverallStatistics(): Map<String, Any> {
        return mapOf(
            "totalRooms" to chatRoomRepository.countActiveRooms(),
            "deletedMessages" to chatMessageRepository.countDeletedMessages()
        )
    }
}

// ========================================
// 확장 함수들
// ========================================

/**
 * ChatRoom 엔티티를 ChatRoomResponse DTO로 변환
 */
fun ChatRoom.toResponse() = ChatRoomResponse(
    id = id,
    name = roomInfo.name,
    description = roomInfo.description,
    maxParticipants = roomInfo.maxParticipants,
    creator = creator.toResponse(),
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * ChatMessage 엔티티를 ChatMessageResponse DTO로 변환
 */
fun ChatMessage.toResponse() = ChatMessageResponse(
    id = id,
    roomId = room.id,
    sender = sender.toResponse(),
    content = getDisplayContent(), // 삭제된 메시지 처리
    messageType = messageContent.messageType,
    fileUrl = if (isDeleted) null else messageContent.fileUrl,
    fileName = if (isDeleted) null else messageContent.fileName,
    fileSize = if (isDeleted) null else messageContent.fileSize,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * ChatRoom을 SimpleChatRoomResponse로 변환
 */
fun ChatRoom.toSimpleResponse() = SimpleChatRoomResponse(
    id = id,
    name = roomInfo.name,
    description = roomInfo.description,
    creatorName = creator.profile.username,
    isActive = isActive,
    createdAt = createdAt
)

/**
 * ChatMessage를 SimpleMessageResponse로 변환
 */
fun ChatMessage.toSimpleResponse() = SimpleMessageResponse(
    id = id,
    roomId = room.id,
    roomName = room.roomInfo.name,
    senderName = sender.profile.username,
    content = getDisplayContent(),
    messageType = messageContent.messageType,
    createdAt = createdAt
)