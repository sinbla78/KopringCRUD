package com.example.kopringCRUD.domain.report.entity


import com.example.kopringCRUD.domain.chat.entity.ChatMessage
import com.example.kopringCRUD.domain.user.entity.User
import jakarta.persistence.Embeddable
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

/**
 * 신고 대상 값 객체
 */
@Embeddable
data class ReportTarget(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    val reportedUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_message_id")
    val reportedMessage: ChatMessage? = null
) {
    init {
        require(reportedUser != null || reportedMessage != null) {
            "신고 대상 (사용자 또는 메시지) 중 하나는 반드시 지정되어야 합니다"
        }

        // 사용자와 메시지 둘 다 지정되어 있는 경우는 허용
        // (특정 사용자의 특정 메시지를 신고하는 경우)
    }

    /**
     * 사용자 신고인지 확인
     */
    fun isUserReport(): Boolean = reportedUser != null

    /**
     * 메시지 신고인지 확인
     */
    fun isMessageReport(): Boolean = reportedMessage != null

    /**
     * 복합 신고인지 확인 (사용자 + 메시지)
     */
    fun isComplexReport(): Boolean = reportedUser != null && reportedMessage != null

    /**
     * 신고 대상의 타입을 문자열로 반환
     */
    fun getTargetType(): String {
        return when {
            isComplexReport() -> "USER_MESSAGE"
            isUserReport() -> "USER"
            isMessageReport() -> "MESSAGE"
            else -> "UNKNOWN"
        }
    }

    /**
     * 신고 대상의 ID를 반환 (메시지 우선)
     */
    fun getTargetId(): Long? {
        return reportedMessage?.id ?: reportedUser?.id
    }

    /**
     * 신고 대상의 설명을 반환
     */
    fun getTargetDescription(): String {
        return when {
            isComplexReport() -> "사용자 '${reportedUser!!.profile.username}'의 메시지 (ID: ${reportedMessage!!.id})"
            isUserReport() -> "사용자 '${reportedUser!!.profile.username}'"
            isMessageReport() -> "메시지 (ID: ${reportedMessage!!.id})"
            else -> "알 수 없는 대상"
        }
    }
}