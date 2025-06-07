package com.example.kopringCRUD.domain.chat.entity

import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.global.common.BaseEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 채팅방 엔티티
 */
@Entity
@Table(name = "chat_rooms")
@EntityListeners(AuditingEntityListener::class)
data class ChatRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Long = 0,

    @Embedded
    val roomInfo: ChatRoomInfo,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    val creator: User,

    val isActive: Boolean = true,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    override val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    override val updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity {

    /**
     * 채팅방을 비활성화합니다.
     */
    fun deactivate(): ChatRoom = this.copy(isActive = false)

    /**
     * 채팅방을 활성화합니다.
     */
    fun activate(): ChatRoom = this.copy(isActive = true)

    /**
     * 채팅방 정보를 업데이트합니다.
     */
    fun updateInfo(newInfo: ChatRoomInfo): ChatRoom = this.copy(roomInfo = newInfo)

    /**
     * 사용자가 이 채팅방의 생성자인지 확인합니다.
     */
    fun isCreatedBy(userId: Long): Boolean = creator.id == userId

    /**
     * 채팅방이 활성 상태인지 확인합니다.
     */
    fun isActiveRoom(): Boolean = isActive

    /**
     * 채팅방 참여 가능 여부를 확인합니다.
     */
    fun canJoin(): Boolean = isActive

    /**
     * 채팅방 이름을 반환합니다.
     */
    fun getName(): String = roomInfo.name

    /**
     * 최대 참여자 수를 반환합니다.
     */
    fun getMaxParticipants(): Int = roomInfo.maxParticipants
}