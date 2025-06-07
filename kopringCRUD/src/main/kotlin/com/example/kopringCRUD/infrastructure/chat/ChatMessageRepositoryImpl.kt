package com.example.kopringCRUD.infrastructure.chat

import com.example.kopringCRUD.domain.chat.entity.ChatMessage
import com.example.kopringCRUD.domain.chat.entity.MessageType
import com.example.kopringCRUD.domain.chat.repository.ChatMessageRepository
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*
import org.springframework.data.domain.PageRequest as SpringPageRequest

/**
 * Spring Data JPA를 위한 ChatMessage 레포지토리 인터페이스
 */
interface JpaChatMessageRepository : JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 조회 (페이징, 최신순)
     */
    fun findByRoomIdOrderByCreatedAtDesc(roomId: Long, pageable: Pageable): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 특정 채팅방의 삭제되지 않은 메시지만 조회 (페이징, 최신순)
     */
    fun findByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(
        roomId: Long,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 특정 채팅방의 삭제되지 않은 메시지만 조회 (페이징, 오래된순 - 채팅 화면용)
     */
    fun findByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(
        roomId: Long,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 특정 사용자가 보낸 메시지 조회 (페이징, 최신순)
     */
    fun findBySenderIdOrderByCreatedAtDesc(senderId: Long, pageable: Pageable): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 특정 채팅방의 특정 사용자 메시지 조회 (페이징, 최신순)
     */
    fun findByRoomIdAndSenderIdOrderByCreatedAtDesc(
        roomId: Long,
        senderId: Long,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 메시지 타입별 조회 (페이징, 최신순)
     */
    fun findByMessageContentMessageTypeOrderByCreatedAtDesc(
        messageType: MessageType,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 특정 채팅방의 메시지 타입별 조회 (페이징, 최신순)
     */
    fun findByRoomIdAndMessageContentMessageTypeOrderByCreatedAtDesc(
        roomId: Long,
        messageType: MessageType,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 메시지 내용으로 검색 (삭제되지 않은 메시지만, 페이징, 최신순)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.messageContent.content LIKE %:keyword% AND cm.isDeleted = false ORDER BY cm.createdAt DESC")
    fun findByContentContainingAndIsDeletedFalseOrderByCreatedAtDesc(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 특정 채팅방에서 메시지 내용으로 검색 (삭제되지 않은 메시지만, 페이징, 최신순)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room.id = :roomId AND cm.messageContent.content LIKE %:keyword% AND cm.isDeleted = false ORDER BY cm.createdAt DESC")
    fun findByRoomIdAndContentContainingAndIsDeletedFalseOrderByCreatedAtDesc(
        @Param("roomId") roomId: Long,
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatMessage>

    /**
     * 특정 채팅방의 전체 메시지 개수
     */
    fun countByRoomId(roomId: Long): Long

    /**
     * 특정 채팅방의 활성 메시지 개수 (삭제되지 않은 메시지)
     */
    fun countByRoomIdAndIsDeletedFalse(roomId: Long): Long

    /**
     * 특정 사용자가 보낸 메시지 개수
     */
    fun countBySenderId(senderId: Long): Long

    /**
     * 삭제된 메시지 개수
     */
    fun countByIsDeletedTrue(): Long

    /**
     * 전체 메시지 개수
     */
    override fun count(): Long

    /**
     * 특정 채팅방의 최근 메시지 조회 (N개, 오래된순)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room.id = :roomId AND cm.isDeleted = false ORDER BY cm.createdAt ASC")
    fun findTopNByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(
        @Param("roomId") roomId: Long,
        pageable: Pageable
    ): List<ChatMessage>

    /**
     * 특정 채팅방의 가장 최근 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room.id = :roomId AND cm.isDeleted = false ORDER BY cm.createdAt DESC LIMIT 1")
    fun findFirstByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("roomId") roomId: Long): Optional<ChatMessage>

    /**
     * 메시지 타입별 개수 통계
     */
    fun countByMessageContentMessageType(messageType: MessageType): Long

    /**
     * 특정 채팅방의 메시지 타입별 개수
     */
    fun countByRoomIdAndMessageContentMessageType(roomId: Long, messageType: MessageType): Long
}

/**
 * 도메인 ChatMessageRepository 인터페이스의 JPA 구현체
 */
@Repository
class ChatMessageRepositoryImpl(
    private val jpaChatMessageRepository: JpaChatMessageRepository
) : ChatMessageRepository {

    override fun findById(id: Long): Optional<ChatMessage> {
        return jpaChatMessageRepository.findById(id)
    }

    override fun findByRoomId(roomId: Long, pageRequest: PageRequest): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable)

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun findActiveMessagesByRoomId(roomId: Long, pageRequest: PageRequest): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        // 채팅 화면에서는 오래된 메시지부터 보여줘야 자연스러움
        val page = jpaChatMessageRepository.findByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(roomId, pageable)

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun findBySenderId(senderId: Long, pageRequest: PageRequest): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatMessageRepository.findBySenderIdOrderByCreatedAtDesc(senderId, pageable)

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun findByRoomIdAndSenderId(
        roomId: Long,
        senderId: Long,
        pageRequest: PageRequest
    ): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatMessageRepository.findByRoomIdAndSenderIdOrderByCreatedAtDesc(roomId, senderId, pageable)

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun findByMessageType(messageType: MessageType, pageRequest: PageRequest): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatMessageRepository.findByMessageContentMessageTypeOrderByCreatedAtDesc(messageType, pageable)

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun findByRoomIdAndMessageType(
        roomId: Long,
        messageType: MessageType,
        pageRequest: PageRequest
    ): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatMessageRepository.findByRoomIdAndMessageContentMessageTypeOrderByCreatedAtDesc(
            roomId, messageType, pageable
        )

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun findByContentContaining(keyword: String, pageRequest: PageRequest): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatMessageRepository.findByContentContainingAndIsDeletedFalseOrderByCreatedAtDesc(
            keyword, pageable
        )

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun findByRoomIdAndContentContaining(
        roomId: Long,
        keyword: String,
        pageRequest: PageRequest
    ): PageResponse<ChatMessage> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatMessageRepository.findByRoomIdAndContentContainingAndIsDeletedFalseOrderByCreatedAtDesc(
            roomId, keyword, pageable
        )

        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    override fun save(message: ChatMessage): ChatMessage {
        return jpaChatMessageRepository.save(message)
    }

    override fun delete(message: ChatMessage) {
        jpaChatMessageRepository.delete(message)
    }

    override fun existsById(id: Long): Boolean {
        return jpaChatMessageRepository.existsById(id)
    }

    override fun countByRoomId(roomId: Long): Long {
        return jpaChatMessageRepository.countByRoomId(roomId)
    }

    override fun countActiveMessagesByRoomId(roomId: Long): Long {
        return jpaChatMessageRepository.countByRoomIdAndIsDeletedFalse(roomId)
    }

    override fun countBySenderId(senderId: Long): Long {
        return jpaChatMessageRepository.countBySenderId(senderId)
    }

    override fun countDeletedMessages(): Long {
        return jpaChatMessageRepository.countByIsDeletedTrue()
    }

    override fun findRecentMessagesByRoomId(roomId: Long, limit: Int): List<ChatMessage> {
        val pageable = SpringPageRequest.of(0, limit)
        return jpaChatMessageRepository.findTopNByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(roomId, pageable)
    }

    override fun findLatestMessageByRoomId(roomId: Long): Optional<ChatMessage> {
        return jpaChatMessageRepository.findFirstByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(roomId)
    }
}