package com.example.kopringCRUD.domain.user.dto

import com.example.kopringCRUD.domain.user.entity.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 사용자 응답 DTO
 */
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)

/**
 * Refresh Token 정보 DTO (활성 기기 목록용)
 */
data class RefreshTokenInfo(
    val id: Long,
    val deviceInfo: String?,
    val ipAddress: String?,
    val createdAt: LocalDateTime,
    val expiryDate: LocalDateTime,
    val isCurrentDevice: Boolean = false,
    val remainingDays: Long = 0
)

/**
 * 사용자 생성 요청 DTO (회원가입)
 */
data class CreateUserRequest(
    @field:NotBlank(message = "사용자명은 필수입니다")
    @field:Size(min = 3, max = 20, message = "사용자명은 3자 이상 20자 이하여야 합니다")
    val username: String,

    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 6, max = 100, message = "비밀번호는 6자 이상 100자 이하여야 합니다")
    val password: String,

    @field:Size(max = 50, message = "표시명은 50자 이하여야 합니다")
    val displayName: String? = null
)

/**
 * 사용자 정보 수정 요청 DTO
 */
data class UpdateUserRequest(
    @field:Size(max = 50, message = "표시명은 50자 이하여야 합니다")
    val displayName: String? = null,

    val avatarUrl: String? = null
)

/**
 * 비밀번호 변경 요청 DTO
 */
data class ChangePasswordRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 6, max = 100, message = "비밀번호는 6자 이상 100자 이하여야 합니다")
    val newPassword: String,

    @field:NotBlank(message = "비밀번호 확인은 필수입니다")
    val confirmPassword: String
) {
    init {
        require(newPassword == confirmPassword) { "새 비밀번호와 확인 비밀번호가 일치하지 않습니다" }
    }
}

/**
 * 로그인 요청 DTO
 */
data class LoginRequest(
    @field:NotBlank(message = "사용자명은 필수입니다")
    val username: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)

/**
 * 인증 응답 DTO (Refresh Token 지원)
 */
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
    val tokenType: String = "Bearer",
    val expiresIn: Long? = null // Access Token 만료 시간 (초 단위, 선택적)
)

/**
 * Refresh Token 요청 DTO
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh Token은 필수입니다")
    val refreshToken: String
)

/**
 * 로그아웃 요청 DTO
 */
data class LogoutRequest(
    @field:NotBlank(message = "Refresh Token은 필수입니다")
    val refreshToken: String
)

/**
 * 토큰 검증 응답 DTO
 */
data class TokenValidationResponse(
    val isValid: Boolean,
    val username: String? = null,
    val expiresAt: LocalDateTime? = null
)

/**
 * 사용자 역할 변경 요청 DTO (관리자 전용)
 */
data class ChangeUserRoleRequest(
    val role: UserRole
)

/**
 * 간단한 사용자 정보 DTO (다른 도메인에서 참조용)
 */
data class SimpleUserResponse(
    val id: Long,
    val username: String,
    val displayName: String?
)

/**
 * 현재 사용자 세션 정보 DTO
 */
data class UserSessionResponse(
    val user: UserResponse,
    val activeTokensCount: Int, // 활성 Refresh Token 개수 (다중 기기 로그인)
    val lastLoginAt: LocalDateTime?
)