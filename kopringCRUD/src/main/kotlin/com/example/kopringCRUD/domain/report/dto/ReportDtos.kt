package com.example.kopringCRUD.domain.report.dto

import com.example.kopringCRUD.domain.chat.dto.ChatMessageResponse
import com.example.kopringCRUD.domain.report.entity.ReportStatus
import com.example.kopringCRUD.domain.report.entity.ReportType
import com.example.kopringCRUD.domain.user.dto.UserResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 신고 응답 DTO
 */
data class ReportResponse(
    val id: Long,
    val reporter: UserResponse,
    val reportedUser: UserResponse?,
    val reportedMessage: ChatMessageResponse?,
    val reportType: ReportType,
    val reportTypeName: String,
    val reason: String,
    val description: String?,
    val status: ReportStatus,
    val statusName: String,
    val priority: Int,
    val urgency: String,
    val handledBy: UserResponse?,
    val handledAt: LocalDateTime?,
    val handlerNote: String?,
    val deadline: LocalDateTime,
    val isOverdue: Boolean,
    val targetType: String,
    val targetDescription: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * 신고 생성 요청 DTO
 */
data class CreateReportRequest(
    val reportedUserId: Long? = null,
    val reportedMessageId: Long? = null,

    val reportType: ReportType,

    @field:NotBlank(message = "신고 사유는 필수입니다")
    @field:Size(max = 200, message = "신고 사유는 200자 이하여야 합니다")
    val reason: String,

    @field:Size(max = 1000, message = "신고 상세 설명은 1000자 이하여야 합니다")
    val description: String? = null
) {
    init {
        require(reportedUserId != null || reportedMessageId != null) {
            "신고 대상 (사용자 ID 또는 메시지 ID) 중 하나는 반드시 지정되어야 합니다"
        }
    }
}

/**
 * 신고 처리 요청 DTO
 */
data class HandleReportRequest(
    val status: ReportStatus,

    @field:Size(max = 500, message = "처리자 메모는 500자 이하여야 합니다")
    val handlerNote: String? = null
) {
    init {
        require(status in listOf(ReportStatus.RESOLVED, ReportStatus.DISMISSED, ReportStatus.ESCALATED, ReportStatus.REVIEWING)) {
            "유효하지 않은 처리 상태입니다"
        }
    }
}

/**
 * 신고 상태 변경 요청 DTO
 */
data class ChangeReportStatusRequest(
    val status: ReportStatus
)

/**
 * 간단한 신고 정보 DTO (목록용)
 */
data class SimpleReportResponse(
    val id: Long,
    val reporterName: String,
    val reportType: ReportType,
    val reportTypeName: String,
    val reason: String,
    val status: ReportStatus,
    val statusName: String,
    val priority: Int,
    val urgency: String,
    val targetType: String,
    val targetDescription: String,
    val isOverdue: Boolean,
    val createdAt: LocalDateTime
)

/**
 * 신고 검색 요청 DTO
 */
data class SearchReportRequest(
    val reporterId: Long? = null,
    val reportedUserId: Long? = null,
    val reportType: ReportType? = null,
    val status: ReportStatus? = null,
    val handledById: Long? = null,
    val isOverdue: Boolean? = null,
    val keyword: String? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null
)

/**
 * 신고 통계 DTO
 */
data class ReportStatistics(
    val totalReports: Long,
    val pendingReports: Long,
    val reviewingReports: Long,
    val resolvedReports: Long,
    val dismissedReports: Long,
    val escalatedReports: Long,
    val overdueReports: Long,
    val reportsByType: Map<ReportType, Long>,
    val averageHandlingTime: Double, // 시간 단위
    val handlingRate: Double // 처리율 (%)
)

/**
 * 사용자별 신고 통계 DTO
 */
data class UserReportStatistics(
    val userId: Long,
    val username: String,
    val reportsSubmitted: Long,
    val reportsReceived: Long,
    val reportsHandled: Long,
    val lastReportDate: LocalDateTime?
)

/**
 * 신고 처리 내역 DTO
 */
data class ReportHandlingHistory(
    val reportId: Long,
    val handlerId: Long,
    val handlerName: String,
    val previousStatus: ReportStatus,
    val newStatus: ReportStatus,
    val handlerNote: String?,
    val handledAt: LocalDateTime
)

/**
 * 신고 유형별 정보 DTO
 */
data class ReportTypeInfo(
    val type: ReportType,
    val displayName: String,
    val description: String,
    val priority: Int,
    val count: Long
)

/**
 * 신고 대시보드 DTO
 */
data class ReportDashboard(
    val statistics: ReportStatistics,
    val recentReports: List<SimpleReportResponse>,
    val overdueReports: List<SimpleReportResponse>,
    val highPriorityReports: List<SimpleReportResponse>,
    val reportTypeDistribution: List<ReportTypeInfo>
)