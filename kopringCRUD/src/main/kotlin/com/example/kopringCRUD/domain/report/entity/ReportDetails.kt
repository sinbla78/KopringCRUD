package com.example.kopringCRUD.domain.report.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * 신고 상세 정보 값 객체
 */
@Embeddable
data class ReportDetails(
    @Enumerated(EnumType.STRING)
    val reportType: ReportType,

    @Column(nullable = false, length = 200)
    val reason: String,

    @Column(length = 1000)
    val description: String? = null,

    @Column(length = 500)
    val handlerNote: String? = null
) {
    init {
        require(reason.isNotBlank()) { "신고 사유는 비어있을 수 없습니다" }
        require(reason.length <= 200) { "신고 사유는 200자 이하여야 합니다" }

        description?.let { desc ->
            require(desc.length <= 1000) { "신고 상세 설명은 1000자 이하여야 합니다" }
        }

        handlerNote?.let { note ->
            require(note.length <= 500) { "처리자 메모는 500자 이하여야 합니다" }
        }
    }

    /**
     * 처리자 메모 추가
     */
    fun addHandlerNote(note: String): ReportDetails {
        require(note.isNotBlank()) { "처리자 메모는 비어있을 수 없습니다" }
        require(note.length <= 500) { "처리자 메모는 500자 이하여야 합니다" }

        return this.copy(handlerNote = note)
    }

    /**
     * 신고 유형의 표시명 반환
     */
    fun getReportTypeName(): String = reportType.displayName

    /**
     * 신고 유형의 설명 반환
     */
    fun getReportTypeDescription(): String = reportType.description

    /**
     * 처리자 메모가 있는지 확인
     */
    fun hasHandlerNote(): Boolean = !handlerNote.isNullOrBlank()

    /**
     * 신고 요약 정보 생성
     */
    fun getSummary(): String {
        val summary = StringBuilder()
        summary.append("[${reportType.displayName}] ")
        summary.append(reason)

        if (!description.isNullOrBlank()) {
            summary.append(" - ")
            summary.append(
                if (description!!.length > 50) {
                    description.substring(0, 50) + "..."
                } else {
                    description
                }
            )
        }

        return summary.toString()
    }
}