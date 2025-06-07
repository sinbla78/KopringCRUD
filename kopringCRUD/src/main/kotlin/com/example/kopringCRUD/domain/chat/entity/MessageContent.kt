package com.example.kopringCRUD.domain.chat.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * 메시지 내용 값 객체
 */
@Embeddable
data class MessageContent(
    @Column(nullable = false, length = 1000)
    val content: String,

    @Enumerated(EnumType.STRING)
    val messageType: MessageType = MessageType.TEXT,

    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null
) {
    init {
        require(content.isNotBlank()) { "메시지 내용은 비어있을 수 없습니다" }
        require(content.length <= 1000) { "메시지 내용은 1000자 이하여야 합니다" }

        // 파일 메시지의 경우 파일 정보 검증
        if (messageType == MessageType.FILE || messageType == MessageType.IMAGE) {
            require(!fileUrl.isNullOrBlank()) { "파일 메시지는 파일 URL이 필요합니다" }
            require(!fileName.isNullOrBlank()) { "파일 메시지는 파일명이 필요합니다" }
        }

        // 파일 크기 검증 (100MB 제한)
        fileSize?.let { size ->
            require(size > 0) { "파일 크기는 0보다 커야 합니다" }
            require(size <= 100_000_000) { "파일 크기는 100MB 이하여야 합니다" }
        }
    }

    /**
     * 파일 메시지인지 확인
     */
    fun isFileMessage(): Boolean = messageType == MessageType.FILE || messageType == MessageType.IMAGE

    /**
     * 시스템 메시지인지 확인
     */
    fun isSystemMessage(): Boolean = messageType == MessageType.SYSTEM

    /**
     * 일반 텍스트 메시지인지 확인
     */
    fun isTextMessage(): Boolean = messageType == MessageType.TEXT
}