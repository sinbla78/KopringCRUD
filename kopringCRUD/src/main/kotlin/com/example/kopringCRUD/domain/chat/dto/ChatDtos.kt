package com.example.kopringCRUD.domain.chat.dto

import com.example.kopringCRUD.domain.chat.entity.MessageType
import com.example.kopringCRUD.domain.user.dto.UserResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 채팅방 응답 DTO
 */
data class ChatRoomResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val maxParticipants: Int,
    val creator: UserResponse,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * 채팅방 생성 요청 DTO
 */
data class CreateChatRoomRequest(
    @field:NotBlank(message = "채팅방 이름은 필수입니다")
    @field:Size(max = 100, message = "채팅방 이름은 100자 이하여야 합니다")
    val name: String,

    @field:Size(max = 500, message = "채팅방 설명은 500자 이하여야 합니다")
    val description: String? = null,

    @field:Positive(message = "최대 참여자 수는 1명 이상이어야 합니다")
    val maxParticipants: Int = 100
) {
    init {
        require(maxParticipants <= 1000) { "최대 참여자 수는 1000명 이하여야 합니다" }
    }
}

/**
 * 채팅방 수정 요청 DTO
 */
data class UpdateChatRoomRequest(
    @field:Size(max = 100, message = "채팅방 이름은 100자 이하여야 합니다")
    val name: String? = null,

    @field:Size(max = 500, message = "채팅방 설명은 500자 이하여야 합니다")
    val description: String? = null,

    @field:Positive(message = "최대 참여자 수는 1명 이상이어야 합니다")
    val maxParticipants: Int? = null
) {
    init {
        maxParticipants?.let { max ->
            require(max <= 1000) { "최대 참여자 수는 1000명 이하여야 합니다" }
        }
    }
}

/**
 * 채팅 메시지 응답 DTO
 */
data class ChatMessageResponse(
    val id: Long,
    val roomId: Long,
    val sender: UserResponse,
    val content: String,
    val messageType: MessageType,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * 메시지 전송 요청 DTO
 */
data class SendMessageRequest(
    @field:NotBlank(message = "메시지 내용은 필수입니다")
    @field:Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다")
    val content: String,

    val messageType: MessageType = MessageType.TEXT,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null
) {
    init {
        // 파일 메시지의 경우 파일 정보 검증
        if (messageType == MessageType.FILE || messageType == MessageType.IMAGE) {
            require(!fileUrl.isNullOrBlank()) { "파일 메시지는 파일 URL이 필요합니다" }
            require(!fileName.isNullOrBlank()) { "파일 메시지는 파일명이 필요합니다" }
        }

        // 파일 크기 검증
        fileSize?.let { size ->
            require(size > 0) { "파일 크기는 0보다 커야 합니다" }
            require(size <= 100_000_000) { "파일 크기는 100MB 이하여야 합니다" }
        }
    }
}

/**
 * 간단한 채팅방 정보 DTO (목록 표시용)
 */
data class SimpleChatRoomResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val creatorName: String,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)

/**
 * 간단한 메시지 정보 DTO (알림용)
 */
data class SimpleMessageResponse(
    val id: Long,
    val roomId: Long,
    val roomName: String,
    val senderName: String,
    val content: String,
    val messageType: MessageType,
    val createdAt: LocalDateTime
)

/**
 * 채팅방 검색 요청 DTO
 */
data class SearchChatRoomRequest(
    val keyword: String? = null,
    val isActive: Boolean? = true,
    val creatorId: Long? = null
)

/**
 * 메시지 검색 요청 DTO
 */
data class SearchMessageRequest(
    val roomId: Long? = null,
    val senderId: Long? = null,
    val messageType: MessageType? = null,
    val keyword: String? = null,
    val isDeleted: Boolean? = false
)

/**
 * 파일 업로드 응답 DTO
 */
data class FileUploadResponse(
    val fileUrl: String,
    val fileName: String,
    val fileSize: Long,
    val contentType: String
)