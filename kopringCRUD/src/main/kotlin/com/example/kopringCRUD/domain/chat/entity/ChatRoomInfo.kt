package com.example.kopringCRUD.domain.chat.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 채팅방 정보 값 객체
 */
@Embeddable
data class ChatRoomInfo(
    @Column(nullable = false)
    val name: String,

    @Column(length = 500)
    val description: String? = null,

    val maxParticipants: Int = 100
) {
    init {
        require(name.isNotBlank()) { "채팅방 이름은 비어있을 수 없습니다" }
        require(name.length <= 100) { "채팅방 이름은 100자 이하여야 합니다" }
        require(maxParticipants > 0) { "최대 참여자 수는 1명 이상이어야 합니다" }
        require(maxParticipants <= 1000) { "최대 참여자 수는 1000명 이하여야 합니다" }

        description?.let { desc ->
            require(desc.length <= 500) { "채팅방 설명은 500자 이하여야 합니다" }
        }
    }
}