package com.example.kopringCRUD.infrastructure.report

import com.example.kopringCRUD.domain.report.entity.Report
import com.example.kopringCRUD.domain.report.entity.ReportStatus
import com.example.kopringCRUD.domain.report.entity.ReportType
import com.example.kopringCRUD.domain.report.repository.ReportRepository
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*
import org.springframework.data.domain.PageRequest as SpringPageRequest

/**
 * Spring Data JPA를 위한 Report 레포지토리 인터페이스
 */
interface JpaReportRepository : JpaRepository<Report, Long> {

    /**
     * 상태별 신고 조회 (페이징, 최신순)
     */
    fun findByStatusOrderByCreatedAtDesc(status: ReportStatus, pageable: Pageable): org.springframework.data.domain.Page<Report>

    /**
     * 신고자별 신고 조회 (페이징, 최신순)
     */
    fun findByReporterIdOrderByCreatedAtDesc(reporterId: Long, pageable: Pageable): org.springframework.data.domain.Page<Report>

    /**
     * 신고 대상 사용자별 신고 조회 (페이징, 최신순)
     */
    fun findByReportTargetReportedUserIdOrderByCreatedAtDesc(
        reportedUserId: Long,
        pageable: Pageable
    ): org.springframework.data.domain.Page<Report>

    /**
     * 신고 유형별 조회 (페이징, 최신순)
     */
    fun findByReportDetailsReportTypeOrderByCreatedAtDesc(
        reportType: ReportType,
        pageable: Pageable
    ): org.springframework.data.domain.Page<Report>

    /**
     * 처리자별 신고 조회 (페이징, 최신순)
     */
    fun findByHandledByIdOrderByCreatedAtDesc(handledById: Long, pageable: Pageable): org.springframework.data.domain.Page<Report>

    /**
     * 모든 신고 조회 (페이징, 최신순)
     */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<Report>

    /**
     * 처리되지 않은 신고 조회 (PENDING, REVIEWING, ESCALATED)
     */
    @Query("SELECT r FROM Report r WHERE r.status IN ('PENDING', 'REVIEWING', 'ESCALATED') ORDER BY r.createdAt DESC")
    fun findUnhandledReportsOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<Report>

    /**
     * 처리된 신고 조회 (RESOLVED, DISMISSED)
     */
    @Query("SELECT r FROM Report r WHERE r.status IN ('RESOLVED', 'DISMISSED') ORDER BY r.createdAt DESC")
    fun findHandledReportsOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<Report>

    /**
     * 기한이 지난 신고 조회 (복잡한 비즈니스 로직으로 JPQL 사용)
     */
    @Query("""
        SELECT r FROM Report r WHERE r.status IN ('PENDING', 'REVIEWING', 'ESCALATED') 
        AND (
            (r.reportDetails.reportType IN ('HATE_SPEECH', 'HARASSMENT', 'PRIVACY_VIOLATION') AND r.createdAt < :oneDayAgo)
            OR (r.reportDetails.reportType = 'INAPPROPRIATE_CONTENT' AND r.createdAt < :twoDaysAgo)
            OR (r.reportDetails.reportType IN ('SPAM', 'FALSE_INFORMATION') AND r.createdAt < :threeDaysAgo)
            OR (r.reportDetails.reportType = 'OTHER' AND r.createdAt < :oneWeekAgo)
        )
        ORDER BY r.createdAt DESC
    """)
    fun findOverdueReportsOrderByCreatedAtDesc(
        @Param("oneDayAgo") oneDayAgo: LocalDateTime,
        @Param("twoDaysAgo") twoDaysAgo: LocalDateTime,
        @Param("threeDaysAgo") threeDaysAgo: LocalDateTime,
        @Param("oneWeekAgo") oneWeekAgo: LocalDateTime,
        pageable: Pageable
    ): org.springframework.data.domain.Page<Report>

    /**
     * 우선순위가 높은 신고 조회 (긴급, 높음 우선순위)
     */
    @Query("SELECT r FROM Report r WHERE r.reportDetails.reportType IN ('HATE_SPEECH', 'HARASSMENT', 'PRIVACY_VIOLATION', 'INAPPROPRIATE_CONTENT') ORDER BY r.createdAt DESC")
    fun findHighPriorityReportsOrderByCreatedAtDesc(pageable: Pageable): org.springframework.data.domain.Page<Report>

    /**
     * 기간별 신고 조회
     */
    fun findByCreatedAtBetweenOrderByCreatedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): org.springframework.data.domain.Page<Report>

    /**
     * 키워드로 신고 검색 (사유 또는 설명에서 검색)
     */
    @Query("SELECT r FROM Report r WHERE r.reportDetails.reason LIKE %:keyword% OR r.reportDetails.description LIKE %:keyword% ORDER BY r.createdAt DESC")
    fun findByKeywordOrderByCreatedAtDesc(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): org.springframework.data.domain.Page<Report>

    /**
     * 중복 신고 확인
     */
    @Query("""
        SELECT r FROM Report r WHERE r.reporter.id = :reporterId 
        AND (:reportedUserId IS NULL OR r.reportTarget.reportedUser.id = :reportedUserId)
        AND (:reportedMessageId IS NULL OR r.reportTarget.reportedMessage.id = :reportedMessageId)
        AND r.reportDetails.reportType = :reportType
        AND r.status IN ('PENDING', 'REVIEWING', 'ESCALATED')
        ORDER BY r.createdAt DESC
    """)
    fun findDuplicateReport(
        @Param("reporterId") reporterId: Long,
        @Param("reportedUserId") reportedUserId: Long?,
        @Param("reportedMessageId") reportedMessageId: Long?,
        @Param("reportType") reportType: ReportType
    ): Optional<Report>

    /**
     * 특정 메시지에 대한 신고 조회
     */
    fun findByReportTargetReportedMessageIdOrderByCreatedAtDesc(
        messageId: Long,
        pageable: Pageable
    ): org.springframework.data.domain.Page<Report>

    /**
     * 최근 N개의 신고 조회
     */
    fun findTopNByOrderByCreatedAtDesc(pageable: Pageable): List<Report>

    // ========================================
    // 통계 관련 메소드들
    // ========================================

    /**
     * 상태별 신고 개수
     */
    fun countByStatus(status: ReportStatus): Long

    /**
     * 신고 유형별 개수
     */
    fun countByReportDetailsReportType(reportType: ReportType): Long

    /**
     * 특정 사용자가 신고한 개수
     */
    fun countByReporterId(reporterId: Long): Long

    /**
     * 특정 사용자가 신고받은 개수
     */
    fun countByReportTargetReportedUserId(reportedUserId: Long): Long

    /**
     * 특정 사용자가 처리한 신고 개수
     */
    fun countByHandledById(handledById: Long): Long

    /**
     * 처리되지 않은 신고 개수
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status IN ('PENDING', 'REVIEWING', 'ESCALATED')")
    fun countUnhandledReports(): Long

    /**
     * 기한이 지난 신고 개수
     */
    @Query("""
        SELECT COUNT(r) FROM Report r WHERE r.status IN ('PENDING', 'REVIEWING', 'ESCALATED') 
        AND (
            (r.reportDetails.reportType IN ('HATE_SPEECH', 'HARASSMENT', 'PRIVACY_VIOLATION') AND r.createdAt < :oneDayAgo)
            OR (r.reportDetails.reportType = 'INAPPROPRIATE_CONTENT' AND r.createdAt < :twoDaysAgo)
            OR (r.reportDetails.reportType IN ('SPAM', 'FALSE_INFORMATION') AND r.createdAt < :threeDaysAgo)
            OR (r.reportDetails.reportType = 'OTHER' AND r.createdAt < :oneWeekAgo)
        )
    """)
    fun countOverdueReports(
        @Param("oneDayAgo") oneDayAgo: LocalDateTime,
        @Param("twoDaysAgo") twoDaysAgo: LocalDateTime,
        @Param("threeDaysAgo") threeDaysAgo: LocalDateTime,
        @Param("oneWeekAgo") oneWeekAgo: LocalDateTime
    ): Long

    /**
     * 기간별 신고 개수
     */
    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    /**
     * 특정 메시지에 대한 신고 개수
     */
    fun countByReportTargetReportedMessageId(messageId: Long): Long

    /**
     * 평균 처리 시간 계산 (H2용) - 수정됨
     */
    @Query("""
        SELECT AVG(TIMESTAMPDIFF(HOUR, r.createdAt, r.handledAt)) 
        FROM Report r 
        WHERE r.handledAt IS NOT NULL
    """)
    fun getAverageHandlingTimeInHoursH2(): Double?

    /**
     * 평균 처리 시간 계산 (MySQL용)
     */
    @Query("""
        SELECT AVG(TIMESTAMPDIFF(HOUR, r.createdAt, r.handledAt)) 
        FROM Report r 
        WHERE r.handledAt IS NOT NULL
    """)
    fun getAverageHandlingTimeInHoursMySQL(): Double?

    /**
     * 처리율 계산
     */
    @Query("""
        SELECT (COUNT(CASE WHEN r.status IN ('RESOLVED', 'DISMISSED') THEN 1 END) * 100.0 / COUNT(*))
        FROM Report r
    """)
    fun getHandlingRate(): Double?

    /**
     * 신고 유형별 통계를 위한 데이터
     */
    @Query("SELECT r.reportDetails.reportType, COUNT(r) FROM Report r GROUP BY r.reportDetails.reportType")
    fun getReportTypeStatisticsData(): List<Array<Any>>

    /**
     * 상태별 통계를 위한 데이터
     */
    @Query("SELECT r.status, COUNT(r) FROM Report r GROUP BY r.status")
    fun getStatusStatisticsData(): List<Array<Any>>
}

/**
 * 도메인 ReportRepository 인터페이스의 JPA 구현체
 */
@Repository
class ReportRepositoryImpl(
    private val jpaReportRepository: JpaReportRepository
) : ReportRepository {

    @Value("\${spring.jpa.database-platform:}")
    private lateinit var databasePlatform: String

    override fun findById(id: Long): Optional<Report> {
        return jpaReportRepository.findById(id)
    }

    override fun findAll(pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findAllByOrderByCreatedAtDesc(pageable)

        return page.toPageResponse()
    }

    override fun findByStatus(status: ReportStatus, pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)

        return page.toPageResponse()
    }

    override fun findByReporterId(reporterId: Long, pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable)

        return page.toPageResponse()
    }

    override fun findByReportedUserId(reportedUserId: Long, pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByReportTargetReportedUserIdOrderByCreatedAtDesc(reportedUserId, pageable)

        return page.toPageResponse()
    }

    override fun findByReportType(reportType: ReportType, pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByReportDetailsReportTypeOrderByCreatedAtDesc(reportType, pageable)

        return page.toPageResponse()
    }

    override fun findByHandledById(handledById: Long, pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByHandledByIdOrderByCreatedAtDesc(handledById, pageable)

        return page.toPageResponse()
    }

    override fun findUnhandledReports(pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findUnhandledReportsOrderByCreatedAtDesc(pageable)

        return page.toPageResponse()
    }

    override fun findHandledReports(pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findHandledReportsOrderByCreatedAtDesc(pageable)

        return page.toPageResponse()
    }

    override fun findOverdueReports(pageRequest: PageRequest): PageResponse<Report> {
        val now = LocalDateTime.now()
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findOverdueReportsOrderByCreatedAtDesc(
            oneDayAgo = now.minusHours(24),
            twoDaysAgo = now.minusHours(48),
            threeDaysAgo = now.minusHours(72),
            oneWeekAgo = now.minusWeeks(1),
            pageable = pageable
        )

        return page.toPageResponse()
    }

    override fun findHighPriorityReports(pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findHighPriorityReportsOrderByCreatedAtDesc(pageable)

        return page.toPageResponse()
    }

    override fun findByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageRequest: PageRequest
    ): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable)

        return page.toPageResponse()
    }

    override fun findByKeyword(keyword: String, pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByKeywordOrderByCreatedAtDesc(keyword, pageable)

        return page.toPageResponse()
    }

    override fun findByMultipleConditions(
        reporterId: Long?,
        reportedUserId: Long?,
        reportType: ReportType?,
        status: ReportStatus?,
        handledById: Long?,
        isOverdue: Boolean?,
        keyword: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        pageRequest: PageRequest
    ): PageResponse<Report> {
        // 복합 조건 검색은 실제 구현에서는 Criteria API나 QueryDSL 사용 권장
        // 여기서는 간단한 구현 예시
        return when {
            keyword != null -> findByKeyword(keyword, pageRequest)
            status != null -> findByStatus(status, pageRequest)
            reportType != null -> findByReportType(reportType, pageRequest)
            reporterId != null -> findByReporterId(reporterId, pageRequest)
            reportedUserId != null -> findByReportedUserId(reportedUserId, pageRequest)
            handledById != null -> findByHandledById(handledById, pageRequest)
            isOverdue == true -> findOverdueReports(pageRequest)
            startDate != null && endDate != null -> findByCreatedAtBetween(startDate, endDate, pageRequest)
            else -> findAll(pageRequest)
        }
    }

    override fun save(report: Report): Report {
        return jpaReportRepository.save(report)
    }

    override fun delete(report: Report) {
        jpaReportRepository.delete(report)
    }

    override fun existsById(id: Long): Boolean {
        return jpaReportRepository.existsById(id)
    }

    override fun findDuplicateReport(
        reporterId: Long,
        reportedUserId: Long?,
        reportedMessageId: Long?,
        reportType: ReportType
    ): Optional<Report> {
        return jpaReportRepository.findDuplicateReport(reporterId, reportedUserId, reportedMessageId, reportType)
    }

    override fun findByReportedMessageId(messageId: Long, pageRequest: PageRequest): PageResponse<Report> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = jpaReportRepository.findByReportTargetReportedMessageIdOrderByCreatedAtDesc(messageId, pageable)

        return page.toPageResponse()
    }

    override fun findRecentReports(limit: Int): List<Report> {
        val pageable = SpringPageRequest.of(0, limit)
        return jpaReportRepository.findTopNByOrderByCreatedAtDesc(pageable)
    }

    // ========================================
    // 통계 관련 메소드들
    // ========================================

    override fun countAll(): Long {
        return jpaReportRepository.count()
    }

    override fun countByStatus(status: ReportStatus): Long {
        return jpaReportRepository.countByStatus(status)
    }

    override fun countByReportType(reportType: ReportType): Long {
        return jpaReportRepository.countByReportDetailsReportType(reportType)
    }

    override fun countByReporterId(reporterId: Long): Long {
        return jpaReportRepository.countByReporterId(reporterId)
    }

    override fun countByReportedUserId(reportedUserId: Long): Long {
        return jpaReportRepository.countByReportTargetReportedUserId(reportedUserId)
    }

    override fun countByHandledById(handledById: Long): Long {
        return jpaReportRepository.countByHandledById(handledById)
    }

    override fun countUnhandledReports(): Long {
        return jpaReportRepository.countUnhandledReports()
    }

    override fun countOverdueReports(): Long {
        val now = LocalDateTime.now()
        return jpaReportRepository.countOverdueReports(
            oneDayAgo = now.minusHours(24),
            twoDaysAgo = now.minusHours(48),
            threeDaysAgo = now.minusHours(72),
            oneWeekAgo = now.minusWeeks(1)
        )
    }

    override fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long {
        return jpaReportRepository.countByCreatedAtBetween(startDate, endDate)
    }

    override fun countByReportedMessageId(messageId: Long): Long {
        return jpaReportRepository.countByReportTargetReportedMessageId(messageId)
    }

    override fun getReportTypeStatistics(): Map<ReportType, Long> {
        val data = jpaReportRepository.getReportTypeStatisticsData()
        return data.associate { row ->
            row[0] as ReportType to row[1] as Long
        }
    }

    override fun getStatusStatistics(): Map<ReportStatus, Long> {
        val data = jpaReportRepository.getStatusStatisticsData()
        return data.associate { row ->
            row[0] as ReportStatus to row[1] as Long
        }
    }

    override fun getAverageHandlingTimeInHours(): Double {
        return if (databasePlatform.contains("H2")) {
            jpaReportRepository.getAverageHandlingTimeInHoursH2() ?: 0.0
        } else {
            jpaReportRepository.getAverageHandlingTimeInHoursMySQL() ?: 0.0
        }
    }

    override fun getHandlingRate(): Double {
        return jpaReportRepository.getHandlingRate() ?: 0.0
    }
}

/**
 * Spring Page를 도메인 PageResponse로 변환하는 확장 함수
 */
private fun org.springframework.data.domain.Page<Report>.toPageResponse(): PageResponse<Report> {
    return PageResponse(
        content = content,
        totalElements = totalElements,
        totalPages = totalPages,
        currentPage = number,
        size = size,
        hasNext = hasNext(),
        hasPrevious = hasPrevious()
    )
}