package com.example.kopringCRUD.domain.chat.repository

import com.example.kopringCRUD.domain.chat.entity.ChatRoom
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import java.util.*

/**
 * 채팅방 레포지토리 인터페이스
 */
interface ChatRoomRepository {

    /**
     * ID로 채팅방 조회
     */
    fun findById(id: Long): Optional<ChatRoom>

    /**
     * 활성 채팅방 목록 조회 (페이징)
     */
    fun findActiveRooms(pageRequest: PageRequest): PageResponse<ChatRoom>

    /**
     * 모든 채팅방 조회 (페이징) - 관리자용
     */
    fun findAllRooms(pageRequest: PageRequest): PageResponse<ChatRoom>

    /**
     * 이름으로 채팅방 검색 (페이징)
     */
    fun findByNameContaining(name: String, pageRequest: PageRequest): PageResponse<ChatRoom>

    /**
     * 특정 사용자가 생성한 채팅방 조회 (페이징)
     */
    fun findByCreatorId(creatorId: Long, pageRequest: PageRequest): PageResponse<ChatRoom>

    /**
     * 특정 사용자가 생성한 활성 채팅방 조회 (페이징)
     */
    fun findActiveRoomsByCreatorId(creatorId: Long, pageRequest: PageRequest): PageResponse<ChatRoom>

    /**
     * 채팅방 저장
     */
    fun save(chatRoom: ChatRoom): ChatRoom

    /**
     * 채팅방 삭제
     */
    fun delete(chatRoom: ChatRoom)

    /**
     * 채팅방 존재 여부 확인
     */
    fun existsById(id: Long): Boolean

    /**
     * 활성 채팅방 개수 조회
     */
    fun countActiveRooms(): Long

    /**
     * 특정 사용자가 생성한 채팅방 개수
     */
    fun countByCreatorId(creatorId: Long): Long
}