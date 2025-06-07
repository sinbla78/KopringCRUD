package com.example.kopringCRUD.domain.report.entity

/**
 * 신고 처리 상태
 */
enum class ReportStatus(
    val displayName: String,
    val description: String
) {
    PENDING("대기중", "신고가 접수되어 검토를 기다리는 상태"),
    REVIEWING("검토중", "관리자가 신고 내용을 검토하고 있는 상태"),
    RESOLVED("해결됨", "신고가 접수되어 적절한 조치가 완료된 상태"),
    DISMISSED("기각됨", "신고 내용 검토 결과 조치가 불필요하다고 판단된 상태"),
    ESCALATED("상급 검토", "복잡한 사안으로 상급 관리자의 검토가 필요한 상태")
}