package com.example.kopringCRUD.global.common

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

interface BaseEntity {
    val id: Long
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}

data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
}

/**
 * 표준 API 응답 래퍼 클래스
 * 모든 REST API 응답을 통일된 형식으로 제공
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val errors: List<String>? = null
) {
    companion object {
        /**
         * 성공 응답 생성
         */
        fun <T> success(message: String, data: T? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message,
                data = data
            )
        }

        /**
         * 데이터 없는 성공 응답 생성
         */
        fun success(message: String): ApiResponse<Unit> {
            return ApiResponse(
                success = true,
                message = message,
                data = Unit
            )
        }

        /**
         * 에러 응답 생성
         */
        fun <T> error(message: String, errors: List<String>? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                message = message,
                errors = errors
            )
        }

        /**
         * 단일 에러 응답 생성
         */
        fun <T> error(message: String, error: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                message = message,
                errors = listOf(error)
            )
        }

        /**
         * Result를 ApiResponse로 변환
         */
        fun <T> fromResult(result: Result<T>, successMessage: String, errorMessage: String = "처리 중 오류가 발생했습니다"): ApiResponse<T> {
            return when (result) {
                is Result.Success -> success(successMessage, result.data)
                is Result.Error -> error(errorMessage, result.exception.message ?: "알 수 없는 오류")
            }
        }
    }
}