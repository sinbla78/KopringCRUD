package com.example.kopringCRUD.domain.user.entity

import com.example.kopringCRUD.global.common.BaseEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Long = 0,

    @Embedded
    val profile: UserProfile,

    @Embedded
    val credentials: UserCredentials,

    @Enumerated(EnumType.STRING)
    val role: UserRole = UserRole.USER,

    val isActive: Boolean = true,

    @Column(name = "last_login_at")
    val lastLoginAt: LocalDateTime? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    override val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    override val updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity {

    // Refresh Token과의 관계 (One-to-Many)
    @OneToMany(mappedBy = "userId", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val refreshTokens: MutableList<RefreshToken> = mutableListOf()

    /**
     * 사용자 계정을 비활성화합니다.
     */
    fun deactivate(): User = this.copy(isActive = false)

    /**
     * 사용자 계정을 활성화합니다.
     */
    fun activate(): User = this.copy(isActive = true)

    /**
     * 사용자 프로필을 업데이트합니다.
     */
    fun updateProfile(newProfile: UserProfile): User = this.copy(profile = newProfile)

    /**
     * 사용자의 비밀번호를 변경합니다.
     */
    fun changePassword(newCredentials: UserCredentials): User = this.copy(credentials = newCredentials)

    /**
     * 사용자의 역할을 변경합니다. (관리자 전용)
     */
    fun changeRole(newRole: UserRole): User = this.copy(role = newRole)

    /**
     * 마지막 로그인 시간을 업데이트합니다.
     */
    fun updateLastLogin(): User = this.copy(lastLoginAt = LocalDateTime.now())

    /**
     * 사용자가 관리자 권한을 가지고 있는지 확인합니다.
     */
    fun isAdmin(): Boolean = role == UserRole.ADMIN

    /**
     * 사용자가 모더레이터 권한을 가지고 있는지 확인합니다.
     */
    fun isModerator(): Boolean = role == UserRole.MODERATOR || role == UserRole.ADMIN

    /**
     * 사용자가 활성 상태인지 확인합니다.
     */
    fun isActiveUser(): Boolean = isActive

    /**
     * 사용자가 특정 권한을 가지고 있는지 확인합니다.
     */
    fun hasRole(targetRole: UserRole): Boolean = role == targetRole

    /**
     * 사용자가 특정 권한 이상을 가지고 있는지 확인합니다.
     */
    fun hasRoleOrHigher(targetRole: UserRole): Boolean {
        return when (targetRole) {
            UserRole.USER -> true
            UserRole.MODERATOR -> role == UserRole.MODERATOR || role == UserRole.ADMIN
            UserRole.ADMIN -> role == UserRole.ADMIN
        }
    }

    /**
     * 현재 활성화된 Refresh Token 개수를 반환합니다.
     */
    fun getActiveRefreshTokenCount(): Int {
        return refreshTokens.count { it.expiryDate.isAfter(LocalDateTime.now()) }
    }

    /**
     * 사용자가 최근에 로그인했는지 확인합니다.
     */
    fun hasRecentLogin(hours: Long = 24): Boolean {
        return lastLoginAt?.isAfter(LocalDateTime.now().minusHours(hours)) ?: false
    }

    /**
     * 사용자 계정이 로그인 가능한 상태인지 확인합니다.
     */
    fun canLogin(): Boolean = isActive

    /**
     * 사용자의 기본 정보를 문자열로 반환합니다.
     */
    override fun toString(): String {
        return "User(id=$id, username=${profile.username}, email=${profile.email}, role=$role, isActive=$isActive)"
    }
}

/**
 * Refresh Token Entity
 */
@Entity
@Table(name = "refresh_tokens", indexes = [
    Index(name = "idx_refresh_token", columnList = "token"),
    Index(name = "idx_user_id", columnList = "user_id"),
    Index(name = "idx_expiry_date", columnList = "expiry_date")
])
@EntityListeners(AuditingEntityListener::class)
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 255)
    val token: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "expiry_date", nullable = false)
    val expiryDate: LocalDateTime,

    @Column(name = "device_info", length = 500)
    val deviceInfo: String? = null, // 기기 정보 (선택적)

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null, // IP 주소 (선택적)

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Refresh Token이 만료되었는지 확인합니다.
     */
    fun isExpired(): Boolean = expiryDate.isBefore(LocalDateTime.now())

    /**
     * Refresh Token이 유효한지 확인합니다.
     */
    fun isValid(): Boolean = !isExpired()

    /**
     * 토큰의 남은 유효 시간을 시간 단위로 반환합니다.
     */
    fun getRemainingHours(): Long {
        val now = LocalDateTime.now()
        return if (expiryDate.isAfter(now)) {
            java.time.Duration.between(now, expiryDate).toHours()
        } else {
            0L
        }
    }
}