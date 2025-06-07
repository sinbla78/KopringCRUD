package com.example.kopringCRUD.global.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null,
    val validationErrors: Map<String, String>? = null
)

sealed class BusinessException(
    message: String,
    val errorCode: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)

class EntityNotFoundException(entityName: String, id: Any) :
    BusinessException("$entityName not found with id: $id", "ENTITY_NOT_FOUND", HttpStatus.NOT_FOUND)

class DuplicateEntityException(entityName: String, field: String, value: Any) :
    BusinessException("$entityName already exists with $field: $value", "DUPLICATE_ENTITY")

class UnauthorizedException(message: String = "Unauthorized access") :
    BusinessException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED)

class ForbiddenException(message: String = "Access forbidden") :
    BusinessException(message, "FORBIDDEN", HttpStatus.FORBIDDEN)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = e.httpStatus.value(),
            error = e.errorCode,
            message = e.message ?: "An error occurred"
        )
        return ResponseEntity.status(e.httpStatus).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val validationErrors = e.bindingResult.fieldErrors.associate { error: FieldError ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "VALIDATION_ERROR",
            message = "Validation failed",
            validationErrors = validationErrors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(e: BadCredentialsException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "INVALID_CREDENTIALS",
            message = "Invalid username or password"
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred"
        )
        return ResponseEntity.internalServerError().body(errorResponse)
    }
}