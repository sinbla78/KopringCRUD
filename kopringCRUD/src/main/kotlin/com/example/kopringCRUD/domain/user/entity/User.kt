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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    override val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    override val updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity {

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
}