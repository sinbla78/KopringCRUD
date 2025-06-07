package com.example.kopringCRUD.infrastructure.user

import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.domain.user.repository.UserRepository
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
 * Spring Data JPA를 위한 User 레포지토리 인터페이스
 */
interface JpaUserRepository : JpaRepository<User, Long> {

    /**
     * 사용자명으로 사용자 조회
     */
    fun findByProfileUsername(username: String): Optional<User>

    /**
     * 이메일로 사용자 조회
     */
    fun findByProfileEmail(email: String): Optional<User>

    /**
     * 사용자명 존재 여부 확인
     */
    fun existsByProfileUsername(username: String): Boolean

    /**
     * 이메일 존재 여부 확인
     */
    fun existsByProfileEmail(email: String): Boolean

    /**
     * 활성 사용자만 조회 (페이징)
     */
    fun findByIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<User>

    /**
     * 역할별 사용자 조회 (페이징)
     */
    fun findByRoleOrderByCreatedAtDesc(role: UserRole, pageable: Pageable): org.springframework.data.domain.Page<User>

    /**
     * 사용자명으로 검색 (페이징)
     */
    @Query("SELECT u FROM User u WHERE u.profile.username LIKE %:username% ORDER BY u.createdAt DESC")
    fun findByUsernameContainingOrderByCreatedAtDesc(
        @Param("username") username: String,
        pageable: Pageable
    ): org.springframework.data.domain.Page<User>

    /**
     * 이메일로 검색 (페이징)
     */
    @Query("SELECT u FROM User u WHERE u.profile.email LIKE %:email% ORDER BY u.createdAt DESC")
    fun findByEmailContainingOrderByCreatedAtDesc(
        @Param("email") email: String,
        pageable: Pageable
    ): org.springframework.data.domain.Page<User>

    /**
     * 모든 사용자 조회 (페이징, 최신순)
     */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<User>
}

/**
 * 도메인 UserRepository 인터페이스의 JPA 구현체
 */
@Repository
class UserRepositoryImpl(
    private val jpaUserRepository: JpaUserRepository
) : UserRepository {

    override fun findById(id: Long): Optional<User> {
        return jpaUserRepository.findById(id)
    }

    override fun findByUsername(username: String): Optional<User> {
        return jpaUserRepository.findByProfileUsername(username)
    }

    override fun findByEmail(email: String): Optional<User> {
        return jpaUserRepository.findByProfileEmail(email)
    }

    override fun existsByUsername(username: String): Boolean {
        return jpaUserRepository.existsByProfileUsername(username)
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaUserRepository.existsByProfileEmail(email)
    }

    override fun save(user: User): User {
        return jpaUserRepository.save(user)
    }

    override fun delete(user: User) {
        jpaUserRepository.delete(user)
    }

    override fun findAll(pageRequest: PageRequest): PageResponse<User> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaUserRepository.findAllByOrderByCreatedAtDesc(pageable)

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

    override fun findByIsActiveTrue(pageRequest: PageRequest): PageResponse<User> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaUserRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)

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

    override fun findByRole(role: UserRole, pageRequest: PageRequest): PageResponse<User> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaUserRepository.findByRoleOrderByCreatedAtDesc(role, pageable)

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

    override fun findByUsernameContaining(username: String, pageRequest: PageRequest): PageResponse<User> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaUserRepository.findByUsernameContainingOrderByCreatedAtDesc(username, pageable)

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

    override fun findByEmailContaining(email: String, pageRequest: PageRequest): PageResponse<User> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaUserRepository.findByEmailContainingOrderByCreatedAtDesc(email, pageable)

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
}