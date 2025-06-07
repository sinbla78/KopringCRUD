package com.example.kopringCRUD.domain.chat.repository

import com.example.kopringCRUD.domain.chat.entity.ChatMessage
import com.example.kopringCRUD.domain.chat.entity.MessageType
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import java.util.*

/**
 * 채팅 메시지 레포지토리 인터페이스
 */
interface ChatMessageRepository {

    /**
     * ID로 메시지 조회
     */
    fun findById(id: Long): Optional<ChatMessage>

    /**
     * 특정 채팅방의 메시지 조회 (페이징, 최신순)
     */
    fun findByRoomId(roomId: Long, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 특정 채팅방의 삭제되지 않은 메시지만 조회 (페이징, 최신순)
     */
    fun findActiveMessagesByRoomId(roomId: Long, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 특정 사용자가 보낸 메시지 조회 (페이징)
     */
    fun findBySenderId(senderId: Long, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 특정 채팅방의 특정 사용자 메시지 조회 (페이징)
     */
    fun findByRoomIdAndSenderId(roomId: Long, senderId: Long, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 메시지 타입별 조회 (페이징)
     */
    fun findByMessageType(messageType: MessageType, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 특정 채팅방의 메시지 타입별 조회 (페이징)
     */
    fun findByRoomIdAndMessageType(roomId: Long, messageType: MessageType, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 메시지 내용으로 검색 (페이징)
     */
    fun findByContentContaining(keyword: String, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 특정 채팅방에서 메시지 내용으로 검색 (페이징)
     */
    fun findByRoomIdAndContentContaining(roomId: Long, keyword: String, pageRequest: PageRequest): PageResponse<ChatMessage>

    /**
     * 메시지 저장
     */
    fun save(message: ChatMessage): ChatMessage

    /**
     * 메시지 삭제
     */
    fun delete(message: ChatMessage)

    /**
     * 메시지 존재 여부 확인
     */
    fun existsById(id: Long): Boolean

    /**
     * 특정 채팅방의 전체 메시지 개수
     */
    fun countByRoomId(roomId: Long): Long

    /**
     * 특정 채팅방의 활성 메시지 개수 (삭제되지 않은 메시지)
     */
    fun countActiveMessagesByRoomId(roomId: Long): Long

    /**
     * 특정 사용자가 보낸 메시지 개수
     */
    fun countBySenderId(senderId: Long): Long

    /**
     * 삭제된 메시지 개수
     */
    fun countDeletedMessages(): Long

    /**
     * 특정 채팅방의 최근 메시지 조회 (N개)
     */
    fun findRecentMessagesByRoomId(roomId: Long, limit: Int): List<ChatMessage>

    /**
     * 특정 채팅방의 가장 최근 메시지 조회
     */
    fun findLatestMessageByRoomId(roomId: Long): Optional<ChatMessage>
}