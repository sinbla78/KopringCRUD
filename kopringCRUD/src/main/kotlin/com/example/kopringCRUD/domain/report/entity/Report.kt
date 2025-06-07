package com.example.kopringCRUD.domain.report.entity

import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.global.common.BaseEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 신고 엔티티
 */
@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener::class)
data class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    val reporter: User,

    @Embedded
    val reportTarget: ReportTarget,

    @Embedded
    val reportDetails: ReportDetails,

    @Enumerated(EnumType.STRING)
    val status: ReportStatus = ReportStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    val handledBy: User? = null,

    val handledAt: LocalDateTime? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    override val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    override val updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity {

    /**
     * 신고를 처리합니다.
     */
    fun handle(handledBy: User, newStatus: ReportStatus, handlerNote: String? = null): Report {
        require(canBeHandledBy(handledBy.role)) { "신고를 처리할 권한이 없습니다" }
        require(canTransitionTo(newStatus)) { "현재 상태에서 ${newStatus.displayName} 상태로 변경할 수 없습니다" }

        val updatedDetails = handlerNote?.let { note ->
            reportDetails.addHandlerNote(note)
        } ?: reportDetails

        return this.copy(
            status = newStatus,
            handledBy = handledBy,
            handledAt = LocalDateTime.now(),
            reportDetails = updatedDetails
        )
    }

    /**
     * 신고 상태를 변경합니다.
     */
    fun changeStatus(newStatus: ReportStatus): Report {
        require(canTransitionTo(newStatus)) { "현재 상태에서 ${newStatus.displayName} 상태로 변경할 수 없습니다" }

        return this.copy(status = newStatus)
    }

    /**
     * 처리자 메모를 추가합니다.
     */
    fun addHandlerNote(note: String): Report {
        val updatedDetails = reportDetails.addHandlerNote(note)
        return this.copy(reportDetails = updatedDetails)
    }

    /**
     * 신고가 처리되었는지 확인합니다.
     */
    fun isHandled(): Boolean = status in listOf(ReportStatus.RESOLVED, ReportStatus.DISMISSED)

    /**
     * 신고가 진행중인지 확인합니다.
     */
    fun isInProgress(): Boolean = status in listOf(ReportStatus.PENDING, ReportStatus.REVIEWING, ReportStatus.ESCALATED)

    /**
     * 신고자가 특정 사용자인지 확인합니다.
     */
    fun isReportedBy(userId: Long): Boolean = reporter.id == userId

    /**
     * 특정 사용자에 의해 처리되었는지 확인합니다.
     */
    fun isHandledBy(userId: Long): Boolean = handledBy?.id == userId

    /**
     * 신고 대상이 특정 사용자인지 확인합니다.
     */
    fun isTargetingUser(userId: Long): Boolean = reportTarget.reportedUser?.id == userId

    /**
     * 신고 대상이 특정 메시지인지 확인합니다.
     */
    fun isTargetingMessage(messageId: Long): Boolean = reportTarget.reportedMessage?.id == messageId

    /**
     * 중복 신고인지 확인합니다.
     */
    fun isDuplicateOf(other: Report): Boolean {
        return reporter.id == other.reporter.id &&
                reportTarget.getTargetId() == other.reportTarget.getTargetId() &&
                reportDetails.reportType == other.reportDetails.reportType
    }

    /**
     * 신고 우선순위를 반환합니다 (높을수록 우선 처리)
     */
    fun getPriority(): Int {
        return when (reportDetails.reportType) {
            ReportType.HATE_SPEECH -> 5
            ReportType.HARASSMENT -> 4
            ReportType.INAPPROPRIATE_CONTENT -> 3
            ReportType.SPAM -> 2
            ReportType.IMPERSONATION -> 3
            ReportType.FALSE_INFORMATION -> 2
            ReportType.PRIVACY_VIOLATION -> 4
            ReportType.OTHER -> 1
        }
    }

    /**
     * 특정 사용자가 이 신고를 처리할 수 있는지 확인합니다.
     */
    private fun canBeHandledBy(userRole: UserRole): Boolean {
        return userRole == UserRole.ADMIN || userRole == UserRole.MODERATOR
    }

    /**
     * 현재 상태에서 새로운 상태로 전환 가능한지 확인합니다.
     */
    private fun canTransitionTo(newStatus: ReportStatus): Boolean {
        return when (status) {
            ReportStatus.PENDING -> newStatus in listOf(
                ReportStatus.REVIEWING,
                ReportStatus.RESOLVED,
                ReportStatus.DISMISSED,
                ReportStatus.ESCALATED
            )
            ReportStatus.REVIEWING -> newStatus in listOf(
                ReportStatus.RESOLVED,
                ReportStatus.DISMISSED,
                ReportStatus.ESCALATED,
                ReportStatus.PENDING  // 재검토를 위해 대기 상태로 되돌림
            )
            ReportStatus.ESCALATED -> newStatus in listOf(
                ReportStatus.RESOLVED,
                ReportStatus.DISMISSED,
                ReportStatus.REVIEWING
            )
            ReportStatus.RESOLVED -> newStatus == ReportStatus.REVIEWING  // 재검토 가능
            ReportStatus.DISMISSED -> newStatus == ReportStatus.REVIEWING  // 재검토 가능
        }
    }

    /**
     * 신고의 긴급도를 반환합니다.
     */
    fun getUrgency(): String {
        val priority = getPriority()
        return when {
            priority >= 4 -> "긴급"
            priority >= 3 -> "높음"
            priority >= 2 -> "보통"
            else -> "낮음"
        }
    }

    /**
     * 처리 기한을 반환합니다 (생성일 기준)
     */
    fun getDeadline(): LocalDateTime {
        val priority = getPriority()
        val hoursToAdd = when {
            priority >= 4 -> 24L  // 긴급: 24시간
            priority >= 3 -> 48L  // 높음: 48시간
            priority >= 2 -> 72L  // 보통: 72시간
            else -> 168L          // 낮음: 7일
        }
        return createdAt.plusHours(hoursToAdd)
    }

    /**
     * 처리 기한이 지났는지 확인합니다.
     */
    fun isOverdue(): Boolean {
        return LocalDateTime.now().isAfter(getDeadline()) && isInProgress()
    }
}