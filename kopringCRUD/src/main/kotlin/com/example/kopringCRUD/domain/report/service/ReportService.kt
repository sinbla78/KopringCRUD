package com.example.kopringCRUD.domain.report.service

import com.example.kopringCRUD.domain.chat.repository.ChatMessageRepository
import com.example.kopringCRUD.domain.chat.service.toResponse
import com.example.kopringCRUD.domain.report.dto.*
import com.example.kopringCRUD.domain.report.entity.Report
import com.example.kopringCRUD.domain.report.entity.ReportDetails
import com.example.kopringCRUD.domain.report.entity.ReportStatus
import com.example.kopringCRUD.domain.report.entity.ReportTarget
import com.example.kopringCRUD.domain.report.entity.ReportType
import com.example.kopringCRUD.domain.report.repository.ReportRepository
import com.example.kopringCRUD.domain.user.repository.UserRepository
import com.example.kopringCRUD.domain.user.service.toResponse
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import com.example.kopringCRUD.global.exception.EntityNotFoundException
import com.example.kopringCRUD.global.exception.ForbiddenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional
class ReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val chatMessageRepository: ChatMessageRepository
) {

    // ========================================
    // 신고 생성 및 조회
    // ========================================

    /**
     * 새로운 신고 생성
     */
    fun createReport(reporterId: Long, request: CreateReportRequest): ReportResponse {
        val reporter = userRepository.findById(reporterId)
            .orElseThrow { EntityNotFoundException("User", reporterId) }

        // 신고 대상 검증
        val reportedUser = request.reportedUserId?.let { userId ->
            userRepository.findById(userId)
                .orElseThrow { EntityNotFoundException("User", userId) }
        }

        val reportedMessage = request.reportedMessageId?.let { messageId ->
            chatMessageRepository.findById(messageId)
                .orElseThrow { EntityNotFoundException("ChatMessage", messageId) }
        }

        // 자기 자신을 신고하는 것 방지
        if (reportedUser?.id == reporterId) {
            throw ForbiddenException("자기 자신을 신고할 수 없습니다")
        }

        // 중복 신고 확인
        val duplicateReport = reportRepository.findDuplicateReport(
            reporterId = reporterId,
            reportedUserId = request.reportedUserId,
            reportedMessageId = request.reportedMessageId,
            reportType = request.reportType
        )

        if (duplicateReport.isPresent) {
            throw ForbiddenException("동일한 내용의 신고가 이미 접수되어 있습니다")
        }

        val report = Report(
            reporter = reporter,
            reportTarget = ReportTarget(
                reportedUser = reportedUser,
                reportedMessage = reportedMessage
            ),
            reportDetails = ReportDetails(
                reportType = request.reportType,
                reason = request.reason,
                description = request.description
            )
        )

        val savedReport = reportRepository.save(report)
        return savedReport.toResponse()
    }

    /**
     * 신고 ID로 조회
     */
    @Transactional(readOnly = true)
    fun findById(id: Long): ReportResponse {
        val report = reportRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Report", id) }
        return report.toResponse()
    }

    /**
     * 모든 신고 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    fun findAll(pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findAll(pageRequest)
        return reports.toResponsePage()
    }

    /**
     * 상태별 신고 조회
     */
    @Transactional(readOnly = true)
    fun findByStatus(status: ReportStatus, pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findByStatus(status, pageRequest)
        return reports.toResponsePage()
    }

    /**
     * 내가 신고한 목록 조회
     */
    @Transactional(readOnly = true)
    fun findByReporterId(reporterId: Long, pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findByReporterId(reporterId, pageRequest)
        return reports.toResponsePage()
    }

    /**
     * 처리되지 않은 신고 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    fun findUnhandledReports(pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findUnhandledReports(pageRequest)
        return reports.toResponsePage()
    }

    /**
     * 기한이 지난 신고 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    fun findOverdueReports(pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findOverdueReports(pageRequest)
        return reports.toResponsePage()
    }

    /**
     * 우선순위가 높은 신고 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    fun findHighPriorityReports(pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findHighPriorityReports(pageRequest)
        return reports.toResponsePage()
    }

    // ========================================
    // 신고 처리
    // ========================================

    /**
     * 신고 처리 (관리자/모더레이터)
     */
    fun handleReport(reportId: Long, handlerId: Long, request: HandleReportRequest): ReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { EntityNotFoundException("Report", reportId) }

        val handler = userRepository.findById(handlerId)
            .orElseThrow { EntityNotFoundException("User", handlerId) }

        val handledReport = reportRepository.save(
            report.handle(handler, request.status, request.handlerNote)
        )

        return handledReport.toResponse()
    }

    /**
     * 신고 상태 변경
     */
    fun changeReportStatus(reportId: Long, request: ChangeReportStatusRequest): ReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { EntityNotFoundException("Report", reportId) }

        val updatedReport = reportRepository.save(report.changeStatus(request.status))
        return updatedReport.toResponse()
    }

    /**
     * 처리자 메모 추가
     */
    fun addHandlerNote(reportId: Long, note: String): ReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { EntityNotFoundException("Report", reportId) }

        val updatedReport = reportRepository.save(report.addHandlerNote(note))
        return updatedReport.toResponse()
    }

    // ========================================
    // 검색 및 필터링
    // ========================================

    /**
     * 복합 조건으로 신고 검색
     */
    @Transactional(readOnly = true)
    fun searchReports(searchRequest: SearchReportRequest, pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findByMultipleConditions(
            reporterId = searchRequest.reporterId,
            reportedUserId = searchRequest.reportedUserId,
            reportType = searchRequest.reportType,
            status = searchRequest.status,
            handledById = searchRequest.handledById,
            isOverdue = searchRequest.isOverdue,
            keyword = searchRequest.keyword,
            startDate = searchRequest.startDate,
            endDate = searchRequest.endDate,
            pageRequest = pageRequest
        )
        return reports.toResponsePage()
    }

    /**
     * 키워드로 신고 검색
     */
    @Transactional(readOnly = true)
    fun searchByKeyword(keyword: String, pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findByKeyword(keyword, pageRequest)
        return reports.toResponsePage()
    }

    // ========================================
    // 통계 및 대시보드
    // ========================================

    /**
     * 신고 통계 조회
     */
    @Transactional(readOnly = true)
    fun getStatistics(): ReportStatistics {
        return ReportStatistics(
            totalReports = reportRepository.countAll(),
            pendingReports = reportRepository.countByStatus(ReportStatus.PENDING),
            reviewingReports = reportRepository.countByStatus(ReportStatus.REVIEWING),
            resolvedReports = reportRepository.countByStatus(ReportStatus.RESOLVED),
            dismissedReports = reportRepository.countByStatus(ReportStatus.DISMISSED),
            escalatedReports = reportRepository.countByStatus(ReportStatus.ESCALATED),
            overdueReports = reportRepository.countOverdueReports(),
            reportsByType = reportRepository.getReportTypeStatistics(),
            averageHandlingTime = reportRepository.getAverageHandlingTimeInHours(),
            handlingRate = reportRepository.getHandlingRate()
        )
    }

    /**
     * 사용자별 신고 통계
     */
    @Transactional(readOnly = true)
    fun getUserStatistics(userId: Long): UserReportStatistics {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User", userId) }

        val recentReports = reportRepository.findByReporterId(userId, PageRequest(0, 1))
        val lastReportDate = if (recentReports.content.isNotEmpty()) {
            recentReports.content[0].createdAt
        } else null

        return UserReportStatistics(
            userId = userId,
            username = user.profile.username,
            reportsSubmitted = reportRepository.countByReporterId(userId),
            reportsReceived = reportRepository.countByReportedUserId(userId),
            reportsHandled = reportRepository.countByHandledById(userId),
            lastReportDate = lastReportDate
        )
    }

    /**
     * 신고 대시보드 데이터
     */
    @Transactional(readOnly = true)
    fun getDashboard(): ReportDashboard {
        val statistics = getStatistics()
        val recentReports = reportRepository.findRecentReports(10).map { it.toSimpleResponse() }
        val overdueReports = reportRepository.findOverdueReports(PageRequest(0, 10)).content.map { it.toSimpleResponse() }
        val highPriorityReports = reportRepository.findHighPriorityReports(PageRequest(0, 10)).content.map { it.toSimpleResponse() }

        val reportTypeDistribution = ReportType.values().map { type ->
            ReportTypeInfo(
                type = type,
                displayName = type.displayName,
                description = type.description,
                priority = when (type) {
                    ReportType.HATE_SPEECH -> 5
                    ReportType.HARASSMENT -> 4
                    ReportType.INAPPROPRIATE_CONTENT -> 3
                    ReportType.SPAM -> 2
                    ReportType.IMPERSONATION -> 3
                    ReportType.FALSE_INFORMATION -> 2
                    ReportType.PRIVACY_VIOLATION -> 4
                    ReportType.OTHER -> 1
                },
                count = reportRepository.countByReportType(type)
            )
        }

        return ReportDashboard(
            statistics = statistics,
            recentReports = recentReports,
            overdueReports = overdueReports,
            highPriorityReports = highPriorityReports,
            reportTypeDistribution = reportTypeDistribution
        )
    }

    // ========================================
    // 유틸리티 메소드
    // ========================================

    /**
     * 신고 삭제 (관리자 전용)
     */
    fun deleteReport(reportId: Long) {
        val report = reportRepository.findById(reportId)
            .orElseThrow { EntityNotFoundException("Report", reportId) }

        reportRepository.delete(report)
    }

    /**
     * 특정 메시지에 대한 신고들 조회
     */
    @Transactional(readOnly = true)
    fun findReportsByMessage(messageId: Long, pageRequest: PageRequest): PageResponse<ReportResponse> {
        val reports = reportRepository.findByReportedMessageId(messageId, pageRequest)
        return reports.toResponsePage()
    }

    /**
     * 신고 처리 내역 조회 (향후 확장 가능)
     */
    @Transactional(readOnly = true)
    fun getHandlingHistory(reportId: Long): List<ReportHandlingHistory> {
        // 실제 구현에서는 별도의 히스토리 테이블이 필요할 수 있음
        // 현재는 기본 구조만 제공
        return emptyList()
    }
}

// ========================================
// 확장 함수들
// ========================================

/**
 * Report 엔티티를 ReportResponse DTO로 변환
 */
fun Report.toResponse() = ReportResponse(
    id = id,
    reporter = reporter.toResponse(),
    reportedUser = reportTarget.reportedUser?.toResponse(),
    reportedMessage = reportTarget.reportedMessage?.toResponse(),
    reportType = reportDetails.reportType,
    reportTypeName = reportDetails.getReportTypeName(),
    reason = reportDetails.reason,
    description = reportDetails.description,
    status = status,
    statusName = status.displayName,
    priority = getPriority(),
    urgency = getUrgency(),
    handledBy = handledBy?.toResponse(),
    handledAt = handledAt,
    handlerNote = reportDetails.handlerNote,
    deadline = getDeadline(),
    isOverdue = isOverdue(),
    targetType = reportTarget.getTargetType(),
    targetDescription = reportTarget.getTargetDescription(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Report 엔티티를 SimpleReportResponse DTO로 변환
 */
fun Report.toSimpleResponse() = SimpleReportResponse(
    id = id,
    reporterName = reporter.profile.username,
    reportType = reportDetails.reportType,
    reportTypeName = reportDetails.getReportTypeName(),
    reason = reportDetails.reason,
    status = status,
    statusName = status.displayName,
    priority = getPriority(),
    urgency = getUrgency(),
    targetType = reportTarget.getTargetType(),
    targetDescription = reportTarget.getTargetDescription(),
    isOverdue = isOverdue(),
    createdAt = createdAt
)

/**
 * PageResponse<Report>를 PageResponse<ReportResponse>로 변환
 */
fun PageResponse<Report>.toResponsePage() = PageResponse(
    content = content.map { it.toResponse() },
    totalElements = totalElements,
    totalPages = totalPages,
    currentPage = currentPage,
    size = size,
    hasNext = hasNext,
    hasPrevious = hasPrevious
)