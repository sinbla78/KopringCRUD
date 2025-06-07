package com.example.kopringCRUD.domain.chat.entity

import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.global.common.BaseEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 채팅 메시지 엔티티
 */
@Entity
@Table(name = "chat_messages")
@EntityListeners(AuditingEntityListener::class)
data class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    val room: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,

    @Embedded
    val messageContent: MessageContent,

    val isDeleted: Boolean = false,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    override val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    override val updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity {

    /**
     * 메시지를 삭제 처리합니다.
     */
    fun delete(): ChatMessage = this.copy(isDeleted = true)

    /**
     * 메시지를 복구합니다.
     */
    fun restore(): ChatMessage = this.copy(isDeleted = false)

    /**
     * 메시지가 삭제된 상태인지 확인합니다.
     */
    fun isDeletedMessage(): Boolean = isDeleted

    /**
     * 사용자가 이 메시지의 작성자인지 확인합니다.
     */
    fun isSentBy(userId: Long): Boolean = sender.id == userId

    /**
     * 메시지가 특정 채팅방에 속하는지 확인합니다.
     */
    fun belongsToRoom(roomId: Long): Boolean = room.id == roomId

    /**
     * 메시지 내용을 반환합니다 (삭제된 경우 대체 텍스트).
     */
    fun getDisplayContent(): String {
        return if (isDeleted) {
            "삭제된 메시지입니다"
        } else {
            messageContent.content
        }
    }

    /**
     * 파일 메시지인지 확인합니다.
     */
    fun isFileMessage(): Boolean = messageContent.isFileMessage()

    /**
     * 시스템 메시지인지 확인합니다.
     */
    fun isSystemMessage(): Boolean = messageContent.isSystemMessage()

    /**
     * 메시지 타입을 반환합니다.
     */
    fun getMessageType(): MessageType = messageContent.messageType

    /**
     * 메시지가 수정 가능한지 확인합니다.
     */
    fun canBeModified(): Boolean = !isDeleted && !isSystemMessage()

    /**
     * 특정 사용자가 이 메시지를 삭제할 수 있는지 확인합니다.
     */
    fun canBeDeletedBy(userId: Long, userRole: com.example.kopringCRUD.domain.user.entity.UserRole): Boolean {
        return when {
            isDeleted -> false  // 이미 삭제된 메시지
            isSentBy(userId) -> true  // 메시지 작성자
            userRole == com.example.kopringCRUD.domain.user.entity.UserRole.ADMIN -> true  // 관리자
            userRole == com.example.kopringCRUD.domain.user.entity.UserRole.MODERATOR -> true  // 모더레이터
            else -> false
        }
    }
}