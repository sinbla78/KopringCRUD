package com.example.kopringCRUD.presentation.report

import com.example.kopringCRUD.domain.report.dto.*
import com.example.kopringCRUD.domain.report.entity.ReportStatus
import com.example.kopringCRUD.domain.report.entity.ReportType
import com.example.kopringCRUD.domain.report.service.ReportService
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import com.example.kopringCRUD.global.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 신고 관리 REST API 컨트롤러
 * 신고 접수, 처리, 조회 기능 제공
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = ["*"])
class ReportController(
    private val reportService: ReportService
) {

    // ========================================
    // 신고 접수 및 조회 (일반 사용자)
    // ========================================

    /**
     * 신고 접수
     *
     * @param userPrincipal 신고자 (현재 인증된 사용자)
     * @param request 신고 내용
     * @return 접수된 신고 정보
     */
    @PostMapping
    fun createReport(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: CreateReportRequest
    ): ResponseEntity<ReportResponse> {
        val report = reportService.createReport(userPrincipal.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(report)
    }

    /**
     * 내가 접수한 신고 목록 조회
     *
     * @param userPrincipal 현재 인증된 사용자
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param status 상태 필터 (선택적)
     * @param reportType 신고 유형 필터 (선택적)
     * @return 내 신고 목록
     */
    @GetMapping("/my")
    fun getMyReports(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: ReportStatus?,
        @RequestParam(required = false) reportType: ReportType?
    ): ResponseEntity<PageResponse<ReportResponse>> {
        val pageRequest = PageRequest(page, size)

        // 기본적으로 신고자 ID로 필터링
        val searchRequest = SearchReportRequest(
            reporterId = userPrincipal.id,
            status = status,
            reportType = reportType
        )

        val reports = reportService.searchReports(searchRequest, pageRequest)
        return ResponseEntity.ok(reports)
    }

    /**
     * 특정 신고 상세 조회
     * 본인이 접수한 신고만 조회 가능 (관리자는 모든 신고 조회 가능)
     *
     * @param userPrincipal 현재 인증된 사용자
     * @param reportId 조회할 신고 ID
     * @return 신고 상세 정보
     */
    @GetMapping("/{reportId}")
    fun getReport(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable reportId: Long
    ): ResponseEntity<ReportResponse> {
        val report = reportService.findById(reportId)

        // 권한 확인: 신고자 본인이거나 관리자/모더레이터만 조회 가능
        val isAuthorized = report.reporter.id == userPrincipal.id ||
                isAdminOrModerator(userPrincipal)

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(report)
    }

    // ========================================
    // 관리자/모더레이터 전용 기능
    // ========================================

    /**
     * 모든 신고 목록 조회 (관리자/모더레이터 전용)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 상태 필터
     * @param reportType 신고 유형 필터
     * @param priority 우선순위 필터 (high, normal, low)
     * @param keyword 검색 키워드
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 신고 목록
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getAllReports(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: ReportStatus?,
        @RequestParam(required = false) reportType: ReportType?,
        @RequestParam(required = false) priority: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<PageResponse<ReportResponse>> {
        val pageRequest = PageRequest(page, size)

        val reports = when (priority) {
            "high" -> reportService.findHighPriorityReports(pageRequest)
            "overdue" -> reportService.findOverdueReports(pageRequest)
            else -> {
                val searchRequest = SearchReportRequest(
                    status = status,
                    reportType = reportType,
                    keyword = keyword,
                    startDate = startDate,
                    endDate = endDate
                )
                reportService.searchReports(searchRequest, pageRequest)
            }
        }

        return ResponseEntity.ok(reports)
    }

    /**
     * 처리되지 않은 신고 목록 조회 (관리자/모더레이터 전용)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 처리 대기중인 신고 목록
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getPendingReports(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<ReportResponse>> {
        val pageRequest = PageRequest(page, size)
        val reports = reportService.findUnhandledReports(pageRequest)
        return ResponseEntity.ok(reports)
    }

    /**
     * 기한이 지난 신고 목록 조회 (관리자/모더레이터 전용)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 기한 초과 신고 목록
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getOverdueReports(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<ReportResponse>> {
        val pageRequest = PageRequest(page, size)
        val reports = reportService.findOverdueReports(pageRequest)
        return ResponseEntity.ok(reports)
    }

    /**
     * 우선순위가 높은 신고 목록 조회 (관리자/모더레이터 전용)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 높은 우선순위 신고 목록
     */
    @GetMapping("/high-priority")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getHighPriorityReports(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<ReportResponse>> {
        val pageRequest = PageRequest(page, size)
        val reports = reportService.findHighPriorityReports(pageRequest)
        return ResponseEntity.ok(reports)
    }

    // ========================================
    // 신고 처리 (관리자/모더레이터 전용)
    // ========================================

    /**
     * 신고 처리 (관리자/모더레이터 전용)
     *
     * @param userPrincipal 처리자 (현재 인증된 관리자/모더레이터)
     * @param reportId 처리할 신고 ID
     * @param request 처리 내용 (상태, 처리자 메모)
     * @return 처리된 신고 정보
     */
    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun handleReport(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable reportId: Long,
        @Valid @RequestBody request: HandleReportRequest
    ): ResponseEntity<ReportResponse> {
        val handledReport = reportService.handleReport(reportId, userPrincipal.id, request)
        return ResponseEntity.ok(handledReport)
    }

    /**
     * 신고 상태 변경 (관리자/모더레이터 전용)
     *
     * @param reportId 상태를 변경할 신고 ID
     * @param request 새로운 상태
     * @return 상태가 변경된 신고 정보
     */
    @PutMapping("/{reportId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun changeReportStatus(
        @PathVariable reportId: Long,
        @Valid @RequestBody request: ChangeReportStatusRequest
    ): ResponseEntity<ReportResponse> {
        val updatedReport = reportService.changeReportStatus(reportId, request)
        return ResponseEntity.ok(updatedReport)
    }

    /**
     * 처리자 메모 추가 (관리자/모더레이터 전용)
     *
     * @param reportId 메모를 추가할 신고 ID
     * @param note 처리자 메모 내용
     * @return 메모가 추가된 신고 정보
     */
    @PutMapping("/{reportId}/note")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun addHandlerNote(
        @PathVariable reportId: Long,
        @RequestBody note: Map<String, String>
    ): ResponseEntity<ReportResponse> {
        val noteContent = note["note"] ?: throw IllegalArgumentException("메모 내용이 필요합니다")
        val updatedReport = reportService.addHandlerNote(reportId, noteContent)
        return ResponseEntity.ok(updatedReport)
    }

    // ========================================
    // 통계 및 대시보드 (관리자/모더레이터 전용)
    // ========================================

    /**
     * 신고 통계 조회 (관리자/모더레이터 전용)
     *
     * @return 신고 관련 전체 통계
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getReportStatistics(): ResponseEntity<ReportStatistics> {
        val statistics = reportService.getStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * 사용자별 신고 통계 조회 (관리자/모더레이터 전용)
     *
     * @param userId 조회할 사용자 ID
     * @return 특정 사용자의 신고 관련 통계
     */
    @GetMapping("/statistics/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getUserReportStatistics(@PathVariable userId: Long): ResponseEntity<UserReportStatistics> {
        val statistics = reportService.getUserStatistics(userId)
        return ResponseEntity.ok(statistics)
    }

    /**
     * 신고 대시보드 데이터 조회 (관리자/모더레이터 전용)
     *
     * @return 대시보드에 필요한 종합 정보
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getReportDashboard(): ResponseEntity<ReportDashboard> {
        val dashboard = reportService.getDashboard()
        return ResponseEntity.ok(dashboard)
    }

    // ========================================
    // 특정 대상에 대한 신고 조회
    // ========================================

    /**
     * 특정 사용자에 대한 신고 목록 조회 (관리자/모더레이터 전용)
     *
     * @param userId 조회할 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 해당 사용자에 대한 신고 목록
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getReportsByUser(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<ReportResponse>> {
        val pageRequest = PageRequest(page, size)
        val searchRequest = SearchReportRequest(reportedUserId = userId)
        val reports = reportService.searchReports(searchRequest, pageRequest)
        return ResponseEntity.ok(reports)
    }

    /**
     * 특정 메시지에 대한 신고 목록 조회 (관리자/모더레이터 전용)
     *
     * @param messageId 조회할 메시지 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 해당 메시지에 대한 신고 목록
     */
    @GetMapping("/message/{messageId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getReportsByMessage(
        @PathVariable messageId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<ReportResponse>> {
        val pageRequest = PageRequest(page, size)
        val reports = reportService.findReportsByMessage(messageId, pageRequest)
        return ResponseEntity.ok(reports)
    }

    // ========================================
    // 관리 기능 (관리자 전용)
    // ========================================

    /**
     * 신고 삭제 (관리자 전용)
     *
     * @param reportId 삭제할 신고 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteReport(@PathVariable reportId: Long): ResponseEntity<Map<String, String>> {
        reportService.deleteReport(reportId)
        return ResponseEntity.ok(
            mapOf(
                "message" to "신고가 성공적으로 삭제되었습니다",
                "deletedReportId" to reportId.toString()
            )
        )
    }

    // ========================================
    // 유틸리티 및 정보 제공
    // ========================================

    /**
     * 신고 유형 목록 조회
     *
     * @return 사용 가능한 신고 유형 목록과 설명
     */
    @GetMapping("/types")
    fun getReportTypes(): ResponseEntity<List<Map<String, Any>>> {
        val reportTypes = ReportType.values().map { type ->
            mapOf(
                "type" to type.name,
                "displayName" to type.displayName,
                "description" to type.description
            )
        }
        return ResponseEntity.ok(reportTypes)
    }

    /**
     * 신고 상태 목록 조회
     *
     * @return 사용 가능한 신고 상태 목록과 설명
     */
    @GetMapping("/statuses")
    fun getReportStatuses(): ResponseEntity<List<Map<String, Any>>> {
        val reportStatuses = ReportStatus.values().map { status ->
            mapOf(
                "status" to status.name,
                "displayName" to status.displayName,
                "description" to status.description
            )
        }
        return ResponseEntity.ok(reportStatuses)
    }

    /**
     * 신고 처리 내역 조회 (향후 확장)
     *
     * @param reportId 처리 내역을 조회할 신고 ID
     * @return 신고 처리 히스토리
     */
    @GetMapping("/{reportId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getReportHistory(@PathVariable reportId: Long): ResponseEntity<List<ReportHandlingHistory>> {
        val history = reportService.getHandlingHistory(reportId)
        return ResponseEntity.ok(history)
    }

    // ========================================
    // 유틸리티 메소드들
    // ========================================

    /**
     * 사용자가 관리자 또는 모더레이터 권한을 가지고 있는지 확인
     */
    private fun isAdminOrModerator(userPrincipal: UserPrincipal): Boolean {
        return userPrincipal.authorities.any { authority ->
            authority.authority in listOf("ROLE_ADMIN", "ROLE_MODERATOR")
        }
    }

    /**
     * API 상태 확인 (헬스체크)
     *
     * @return API 상태 정보
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "Report Management API",
                "timestamp" to System.currentTimeMillis(),
                "endpoints" to listOf(
                    mapOf("method" to "POST", "path" to "/api/reports", "description" to "신고 접수"),
                    mapOf("method" to "GET", "path" to "/api/reports/my", "description" to "내 신고 목록"),
                    mapOf("method" to "GET", "path" to "/api/reports", "description" to "전체 신고 목록 (관리자)"),
                    mapOf("method" to "PUT", "path" to "/api/reports/{id}", "description" to "신고 처리 (관리자)"),
                    mapOf("method" to "GET", "path" to "/api/reports/pending", "description" to "처리 대기 신고"),
                    mapOf("method" to "GET", "path" to "/api/reports/overdue", "description" to "기한 초과 신고"),
                    mapOf("method" to "GET", "path" to "/api/reports/statistics", "description" to "신고 통계"),
                    mapOf("method" to "GET", "path" to "/api/reports/dashboard", "description" to "관리자 대시보드"),
                    mapOf("method" to "GET", "path" to "/api/reports/types", "description" to "신고 유형 목록"),
                    mapOf("method" to "GET", "path" to "/api/reports/statuses", "description" to "신고 상태 목록")
                )
            )
        )
    }
}