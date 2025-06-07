package com.example.kopringCRUD.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class UserCredentials(
    @Column(nullable = false)
    val password: String
) {
    init {
        require(password.isNotBlank()) { "비밀번호는 비어있을 수 없습니다" }
        // 실제로는 이미 암호화된 비밀번호가 들어오므로 길이 검증은 서비스 레이어에서 수행
    }
}