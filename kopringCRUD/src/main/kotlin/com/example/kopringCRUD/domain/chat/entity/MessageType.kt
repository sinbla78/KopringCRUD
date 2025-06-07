package com.example.kopringCRUD.domain.chat.entity

enum class MessageType {
    TEXT,     // 일반 텍스트 메시지
    IMAGE,    // 이미지 메시지
    FILE,     // 파일 메시지
    SYSTEM    // 시스템 메시지 (입장/퇴장 알림 등)
}