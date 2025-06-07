package com.example.kopringCRUD.domain.report.repository

import com.example.kopringCRUD.domain.report.entity.Report
import com.example.kopringCRUD.domain.report.entity.ReportStatus
import com.example.kopringCRUD.domain.report.entity.ReportType
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import java.time.LocalDateTime
import java.util.*

/**
 * 신고 레포지토리 인터페이스
 */
interface ReportRepository {

    /**
     * ID로 신고 조회
     */
    fun findById(id: Long): Optional<Report>

    /**
     * 모든 신고 조회 (페이징)
     */
    fun findAll(pageRequest: PageRequest): PageResponse<Report>

    /**
     * 상태별 신고 조회 (페이징)
     */
    fun findByStatus(status: ReportStatus, pageRequest: PageRequest): PageResponse<Report>

    /**
     * 신고자별 신고 조회 (페이징)
     */
    fun findByReporterId(reporterId: Long, pageRequest: PageRequest): PageResponse<Report>

    /**
     * 신고 대상 사용자별 신고 조회 (페이징)
     */
    fun findByReportedUserId(reportedUserId: Long, pageRequest: PageRequest): PageResponse<Report>

    /**
     * 신고 유형별 조회 (페이징)
     */
    fun findByReportType(reportType: ReportType, pageRequest: PageRequest): PageResponse<Report>

    /**
     * 처리자별 신고 조회 (페이징)
     */
    fun findByHandledById(handledById: Long, pageRequest: PageRequest): PageResponse<Report>

    /**
     * 처리되지 않은 신고 조회 (페이징)
     */
    fun findUnhandledReports(pageRequest: PageRequest): PageResponse<Report>

    /**
     * 처리된 신고 조회 (페이징)
     */
    fun findHandledReports(pageRequest: PageRequest): PageResponse<Report>

    /**
     * 기한이 지난 신고 조회 (페이징)
     */
    fun findOverdueReports(pageRequest: PageRequest): PageResponse<Report>

    /**
     * 우선순위가 높은 신고 조회 (페이징)
     */
    fun findHighPriorityReports(pageRequest: PageRequest): PageResponse<Report>

    /**
     * 기간별 신고 조회 (페이징)
     */
    fun findByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageRequest: PageRequest
    ): PageResponse<Report>

    /**
     * 키워드로 신고 검색 (사유 또는 설명에서 검색)
     */
    fun findByKeyword(keyword: String, pageRequest: PageRequest): PageResponse<Report>

    /**
     * 복합 조건 검색
     */
    fun findByMultipleConditions(
        reporterId: Long? = null,
        reportedUserId: Long? = null,
        reportType: ReportType? = null,
        status: ReportStatus? = null,
        handledById: Long? = null,
        isOverdue: Boolean? = null,
        keyword: String? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        pageRequest: PageRequest
    ): PageResponse<Report>

    /**
     * 신고 저장
     */
    fun save(report: Report): Report

    /**
     * 신고 삭제
     */
    fun delete(report: Report)

    /**
     * 신고 존재 여부 확인
     */
    fun existsById(id: Long): Boolean

    /**
     * 중복 신고 확인
     */
    fun findDuplicateReport(
        reporterId: Long,
        reportedUserId: Long?,
        reportedMessageId: Long?,
        reportType: ReportType
    ): Optional<Report>

    // ========================================
    // 통계 관련 메소드들
    // ========================================

    /**
     * 전체 신고 개수
     */
    fun countAll(): Long

    /**
     * 상태별 신고 개수
     */
    fun countByStatus(status: ReportStatus): Long

    /**
     * 신고 유형별 개수
     */
    fun countByReportType(reportType: ReportType): Long

    /**
     * 특정 사용자가 신고한 개수
     */
    fun countByReporterId(reporterId: Long): Long

    /**
     * 특정 사용자가 신고받은 개수
     */
    fun countByReportedUserId(reportedUserId: Long): Long

    /**
     * 특정 사용자가 처리한 신고 개수
     */
    fun countByHandledById(handledById: Long): Long

    /**
     * 처리되지 않은 신고 개수
     */
    fun countUnhandledReports(): Long

    /**
     * 기한이 지난 신고 개수
     */
    fun countOverdueReports(): Long

    /**
     * 기간별 신고 개수
     */
    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    /**
     * 신고 유형별 통계 (Map 형태)
     */
    fun getReportTypeStatistics(): Map<ReportType, Long>

    /**
     * 상태별 통계 (Map 형태)
     */
    fun getStatusStatistics(): Map<ReportStatus, Long>

    /**
     * 평균 처리 시간 (시간 단위)
     */
    fun getAverageHandlingTimeInHours(): Double

    /**
     * 처리율 계산 (%)
     */
    fun getHandlingRate(): Double

    /**
     * 최근 N개의 신고 조회
     */
    fun findRecentReports(limit: Int): List<Report>

    /**
     * 특정 메시지에 대한 신고 조회
     */
    fun findByReportedMessageId(messageId: Long, pageRequest: PageRequest): PageResponse<Report>

    /**
     * 특정 메시지에 대한 신고 개수
     */
    fun countByReportedMessageId(messageId: Long): Long
}