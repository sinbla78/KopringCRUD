package com.example.kopringCRUD.infrastructure.chat

import com.example.kopringCRUD.domain.chat.entity.ChatRoom
import com.example.kopringCRUD.domain.chat.repository.ChatRoomRepository
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
 * Spring Data JPA를 위한 ChatRoom 레포지토리 인터페이스
 */
interface JpaChatRoomRepository : JpaRepository<ChatRoom, Long> {

    /**
     * 활성 채팅방만 조회 (페이징, 최신순)
     */
    fun findByIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<ChatRoom>

    /**
     * 모든 채팅방 조회 (페이징, 최신순)
     */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<ChatRoom>

    /**
     * 이름으로 채팅방 검색 (활성 채팅방만, 페이징)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.roomInfo.name LIKE %:name% AND cr.isActive = true ORDER BY cr.createdAt DESC")
    fun findByNameContainingAndIsActiveTrueOrderByCreatedAtDesc(
        @Param("name") name: String,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatRoom>

    /**
     * 이름으로 채팅방 검색 (모든 채팅방, 페이징)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.roomInfo.name LIKE %:name% ORDER BY cr.createdAt DESC")
    fun findByNameContainingOrderByCreatedAtDesc(
        @Param("name") name: String,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatRoom>

    /**
     * 특정 사용자가 생성한 채팅방 조회 (페이징, 최신순)
     */
    fun findByCreatorIdOrderByCreatedAtDesc(creatorId: Long, pageable: Pageable): org.springframework.data.domain.Page<ChatRoom>

    /**
     * 특정 사용자가 생성한 활성 채팅방 조회 (페이징, 최신순)
     */
    fun findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(
        creatorId: Long,
        pageable: Pageable
    ): org.springframework.data.domain.Page<ChatRoom>

    /**
     * 활성 채팅방 개수 조회
     */
    fun countByIsActiveTrue(): Long

    /**
     * 특정 사용자가 생성한 채팅방 개수
     */
    fun countByCreatorId(creatorId: Long): Long

    /**
     * 특정 사용자가 생성한 활성 채팅방 개수
     */
    fun countByCreatorIdAndIsActiveTrue(creatorId: Long): Long
}

/**
 * 도메인 ChatRoomRepository 인터페이스의 JPA 구현체
 */
@Repository
class ChatRoomRepositoryImpl(
    private val jpaChatRoomRepository: JpaChatRoomRepository
) : ChatRoomRepository {

    override fun findById(id: Long): Optional<ChatRoom> {
        return jpaChatRoomRepository.findById(id)
    }

    override fun findActiveRooms(pageRequest: PageRequest): PageResponse<ChatRoom> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatRoomRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)

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

    override fun findAllRooms(pageRequest: PageRequest): PageResponse<ChatRoom> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatRoomRepository.findAllByOrderByCreatedAtDesc(pageable)

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

    override fun findByNameContaining(name: String, pageRequest: PageRequest): PageResponse<ChatRoom> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatRoomRepository.findByNameContainingAndIsActiveTrueOrderByCreatedAtDesc(name, pageable)

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

    override fun findByCreatorId(creatorId: Long, pageRequest: PageRequest): PageResponse<ChatRoom> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatRoomRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId, pageable)

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

    override fun findActiveRoomsByCreatorId(creatorId: Long, pageRequest: PageRequest): PageResponse<ChatRoom> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaChatRoomRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(creatorId, pageable)

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

    override fun save(chatRoom: ChatRoom): ChatRoom {
        return jpaChatRoomRepository.save(chatRoom)
    }

    override fun delete(chatRoom: ChatRoom) {
        jpaChatRoomRepository.delete(chatRoom)
    }

    override fun existsById(id: Long): Boolean {
        return jpaChatRoomRepository.existsById(id)
    }

    override fun countActiveRooms(): Long {
        return jpaChatRoomRepository.countByIsActiveTrue()
    }

    override fun countByCreatorId(creatorId: Long): Long {
        return jpaChatRoomRepository.countByCreatorId(creatorId)
    }
}