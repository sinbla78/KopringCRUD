package com.example.kopringCRUD.domain.user.service


import com.example.kopringCRUD.domain.user.dto.ChangePasswordRequest
import com.example.kopringCRUD.domain.user.dto.ChangeUserRoleRequest
import com.example.kopringCRUD.domain.user.dto.CreateUserRequest
import com.example.kopringCRUD.domain.user.dto.SimpleUserResponse
import com.example.kopringCRUD.domain.user.dto.UpdateUserRequest
import com.example.kopringCRUD.domain.user.dto.UserResponse
import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.domain.user.entity.UserCredentials
import com.example.kopringCRUD.domain.user.entity.UserProfile
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.domain.user.repository.UserRepository
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import com.example.kopringCRUD.global.exception.DuplicateEntityException
import com.example.kopringCRUD.global.exception.EntityNotFoundException
import com.example.kopringCRUD.global.exception.ForbiddenException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * 새 사용자 생성 (회원가입)
     */
    fun createUser(request: CreateUserRequest): UserResponse {
        // 중복 검사
        if (userRepository.existsByUsername(request.username)) {
            throw DuplicateEntityException("User", "username", request.username)
        }

        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEntityException("User", "email", request.email)
        }

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(request.password)

        // 사용자 엔티티 생성
        val user = User(
            profile = UserProfile(
                username = request.username,
                email = request.email,
                displayName = request.displayName
            ),
            credentials = UserCredentials(
                password = encodedPassword
            )
        )

        val savedUser = userRepository.save(user)
        return savedUser.toResponse()
    }

    /**
     * ID로 사용자 조회
     */
    @Transactional(readOnly = true)
    fun findById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User", id) }
        return user.toResponse()
    }

    /**
     * 사용자명으로 사용자 조회
     */
    @Transactional(readOnly = true)
    fun findByUsername(username: String): UserResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("User", username) }
        return user.toResponse()
    }

    /**
     * 모든 사용자 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun findAllUsers(pageRequest: PageRequest): PageResponse<UserResponse> {
        val users = userRepository.findAll(pageRequest)
        return PageResponse(
            content = users.content.map { it.toResponse() },
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            currentPage = users.currentPage,
            size = users.size,
            hasNext = users.hasNext,
            hasPrevious = users.hasPrevious
        )
    }

    /**
     * 활성 사용자만 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun findActiveUsers(pageRequest: PageRequest): PageResponse<UserResponse> {
        val users = userRepository.findByIsActiveTrue(pageRequest)
        return PageResponse(
            content = users.content.map { it.toResponse() },
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            currentPage = users.currentPage,
            size = users.size,
            hasNext = users.hasNext,
            hasPrevious = users.hasPrevious
        )
    }

    /**
     * 사용자 프로필 수정
     */
    fun updateUser(id: Long, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User", id) }

        val updatedProfile = user.profile.copy(
            displayName = request.displayName ?: user.profile.displayName,
            avatarUrl = request.avatarUrl ?: user.profile.avatarUrl
        )

        val updatedUser = userRepository.save(user.updateProfile(updatedProfile))
        return updatedUser.toResponse()
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(id: Long, request: ChangePasswordRequest): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User", id) }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword, user.credentials.password)) {
            throw ForbiddenException("현재 비밀번호가 일치하지 않습니다")
        }

        // 새 비밀번호 암호화
        val encodedNewPassword = passwordEncoder.encode(request.newPassword)
        val newCredentials = UserCredentials(password = encodedNewPassword)

        val updatedUser = userRepository.save(user.changePassword(newCredentials))
        return updatedUser.toResponse()
    }

    /**
     * 사용자 계정 비활성화
     */
    fun deactivateUser(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User", id) }

        val deactivatedUser = userRepository.save(user.deactivate())
        return deactivatedUser.toResponse()
    }

    /**
     * 사용자 계정 활성화
     */
    fun activateUser(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User", id) }

        val activatedUser = userRepository.save(user.activate())
        return activatedUser.toResponse()
    }

    /**
     * 사용자 역할 변경 (관리자 전용)
     */
    fun changeUserRole(id: Long, request: ChangeUserRoleRequest): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User", id) }

        val updatedUser = userRepository.save(user.changeRole(request.role))
        return updatedUser.toResponse()
    }

    /**
     * 사용자명으로 검색 (페이징)
     */
    @Transactional(readOnly = true)
    fun searchByUsername(username: String, pageRequest: PageRequest): PageResponse<UserResponse> {
        val users = userRepository.findByUsernameContaining(username, pageRequest)
        return PageResponse(
            content = users.content.map { it.toResponse() },
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            currentPage = users.currentPage,
            size = users.size,
            hasNext = users.hasNext,
            hasPrevious = users.hasPrevious
        )
    }

    /**
     * 역할별 사용자 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun findByRole(role: UserRole, pageRequest: PageRequest): PageResponse<UserResponse> {
        val users = userRepository.findByRole(role, pageRequest)
        return PageResponse(
            content = users.content.map { it.toResponse() },
            totalElements = users.totalElements,
            totalPages = users.totalPages,
            currentPage = users.currentPage,
            size = users.size,
            hasNext = users.hasNext,
            hasPrevious = users.hasPrevious
        )
    }

    /**
     * 사용자 삭제 (관리자 전용)
     */
    fun deleteUser(id: Long) {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User", id) }

        userRepository.delete(user)
    }
}

/**
 * User 엔티티를 UserResponse DTO로 변환
 */
fun User.toResponse() = UserResponse(
    id = id,
    username = profile.username,
    email = profile.email,
    displayName = profile.displayName,
    avatarUrl = profile.avatarUrl,
    role = role,
    isActive = isActive,
    createdAt = createdAt
)

/**
 * User 엔티티를 SimpleUserResponse DTO로 변환
 */
fun User.toSimpleResponse() = SimpleUserResponse(
    id = id,
    username = profile.username,
    displayName = profile.displayName
)