package com.example.kopringCRUD.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class UserProfile(
    @Column(unique = true, nullable = false)
    val username: String,

    @Column(unique = true, nullable = false)
    val email: String,

    val displayName: String? = null,
    val avatarUrl: String? = null
) {
    init {
        require(username.isNotBlank()) { "사용자명은 비어있을 수 없습니다" }
        require(username.length >= 3) { "사용자명은 3자 이상이어야 합니다" }
        require(username.length <= 20) { "사용자명은 20자 이하여야 합니다" }

        require(email.isNotBlank()) { "이메일은 비어있을 수 없습니다" }
        require(email.contains("@")) { "올바른 이메일 형식이어야 합니다" }

        displayName?.let {
            require(it.length <= 50) { "표시명은 50자 이하여야 합니다" }
        }
    }
}