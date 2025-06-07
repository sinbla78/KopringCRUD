package com.example.kopringCRUD.domain.report.entity;

/**
 * 신고 유형
 */
enum class ReportType(
    val displayName: String,
    val description: String
) {
    SPAM("스팸", "광고성 메시지나 반복적인 불필요한 콘텐츠"),
    HARASSMENT("괴롭힘", "다른 사용자를 괴롭히거나 위협하는 행위"),
    INAPPROPRIATE_CONTENT("부적절한 콘텐츠", "음란물, 폭력적 내용 등 부적절한 콘텐츠"),
    HATE_SPEECH("혐오 발언", "특정 개인이나 집단에 대한 차별적, 혐오적 발언"),
    IMPERSONATION("사칭", "다른 사용자나 유명인을 사칭하는 행위"),
    FALSE_INFORMATION("허위 정보", "고의적으로 잘못된 정보를 유포하는 행위"),
    PRIVACY_VIOLATION("개인정보 침해", "동의 없이 개인정보를 공개하는 행위"),
    OTHER("기타", "기타 이용약관 위반 행위")
}