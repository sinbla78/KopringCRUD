package com.example.kopringCRUD.domain.user.repository

import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import java.util.*

/**
 * 사용자 레포지토리 인터페이스
 * - 도메인 레이어에서 정의하여 의존성 역전 원칙 적용
 * - Infrastructure 레이어에서 JPA 구현체 제공
 */
interface UserRepository {

    /**
     * ID로 사용자 조회
     */
    fun findById(id: Long): Optional<User>

    /**
     * 사용자명으로 사용자 조회
     */
    fun findByUsername(username: String): Optional<User>

    /**
     * 이메일로 사용자 조회
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * 사용자명 중복 확인
     */
    fun existsByUsername(username: String): Boolean

    /**
     * 이메일 중복 확인
     */
    fun existsByEmail(email: String): Boolean

    /**
     * 사용자 저장
     */
    fun save(user: User): User

    /**
     * 사용자 삭제
     */
    fun delete(user: User)

    /**
     * 모든 사용자 조회 (페이징)
     */
    fun findAll(pageRequest: PageRequest): PageResponse<User>

    /**
     * 활성 사용자만 조회 (페이징)
     */
    fun findByIsActiveTrue(pageRequest: PageRequest): PageResponse<User>

    /**
     * 역할별 사용자 조회 (페이징)
     */
    fun findByRole(role: UserRole, pageRequest: PageRequest): PageResponse<User>

    /**
     * 사용자명으로 검색 (페이징)
     */
    fun findByUsernameContaining(username: String, pageRequest: PageRequest): PageResponse<User>

    /**
     * 이메일로 검색 (페이징)
     */
    fun findByEmailContaining(email: String, pageRequest: PageRequest): PageResponse<User>
}