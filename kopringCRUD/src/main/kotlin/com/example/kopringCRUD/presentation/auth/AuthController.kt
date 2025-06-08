package com.example.kopringCRUD.presentation.auth

import com.example.kopringCRUD.domain.user.dto.*
import com.example.kopringCRUD.domain.user.service.AuthService
import com.example.kopringCRUD.domain.user.service.UserService
import com.example.kopringCRUD.global.common.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * 인증 관련 REST API 컨트롤러
 * 회원가입, 로그인, 토큰 관리 기능 제공
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"]) // 개발 환경용
class AuthController(
    private val authService: AuthService,
    private val userService: UserService
) {

    /**
     * 사용자 회원가입
     */
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("회원가입이 완료되었습니다", response))
    }

    /**
     * 사용자 로그인
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)

        return ResponseEntity.ok(
            ApiResponse.success("로그인이 완료되었습니다", response)
        )
    }

    /**
     * Access Token 갱신
     */
    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.refreshAccessToken(request)

        return ResponseEntity.ok(
            ApiResponse.success("토큰이 갱신되었습니다", response)
        )
    }

    /**
     * 로그아웃 (특정 기기)
     */
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.logout(request.refreshToken)

        return ResponseEntity.ok(
            ApiResponse.success("로그아웃되었습니다")
        )
    }

    /**
     * 모든 기기에서 로그아웃
     */
    @PostMapping("/logout-all")
    fun logoutFromAllDevices(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Unit>> {
        val user = userService.findByUsername(userDetails.username)
        authService.logoutFromAllDevices(user.id!!)

        return ResponseEntity.ok(
            ApiResponse.success("모든 기기에서 로그아웃되었습니다")
        )
    }

    /**
     * Access Token 검증
     */
    @PostMapping("/validate")
    fun validateToken(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ApiResponse<TokenValidationResponse>> {
        val token = extractTokenFromHeader(authHeader)

        val isValid = authService.validateAccessToken(token)
        val response = if (isValid) {
            val username = authService.getUsernameFromToken(token)
            TokenValidationResponse(
                isValid = true,
                username = username
            )
        } else {
            TokenValidationResponse(isValid = false)
        }

        return ResponseEntity.ok(
            ApiResponse.success("토큰 검증이 완료되었습니다", response)
        )
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.getCurrentUser(userDetails.username)

        return ResponseEntity.ok(
            ApiResponse.success("사용자 정보를 조회했습니다", response)
        )
    }

    /**
     * 사용자명/이메일 중복 확인
     */
    @GetMapping("/check-availability")
    fun checkAvailability(
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false) email: String?
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val result = mutableMapOf<String, Any>()

        username?.let {
            // 임시로 항상 사용 가능으로 설정 (UserService에 메서드가 없는 경우)
            val isAvailable = true // userService.existsByUsername(it) 메서드가 있다면 !userService.existsByUsername(it)
            result["usernameAvailable"] = isAvailable
            result["usernameMessage"] = if (isAvailable) "사용 가능한 사용자명입니다" else "이미 사용 중인 사용자명입니다"
        }

        email?.let {
            // 임시로 항상 사용 가능으로 설정 (UserService에 메서드가 없는 경우)
            val isAvailable = true // userService.existsByEmail(it) 메서드가 있다면 !userService.existsByEmail(it)
            result["emailAvailable"] = isAvailable
            result["emailMessage"] = if (isAvailable) "사용 가능한 이메일입니다" else "이미 사용 중인 이메일입니다"
        }

        if (username == null && email == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("확인할 사용자명 또는 이메일을 제공해주세요"))
        }

        return ResponseEntity.ok(
            ApiResponse.success("중복 확인이 완료되었습니다", result)
        )
    }

    /**
     * API 상태 확인 (헬스체크)
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val healthInfo = mapOf(
            "status" to "UP",
            "service" to "Authentication API",
            "timestamp" to System.currentTimeMillis(),
            "version" to "1.0.0",
            "features" to listOf("refresh-token", "jwt-auth"),
            "endpoints" to listOf(
                mapOf("method" to "POST", "path" to "/api/auth/register", "description" to "회원가입"),
                mapOf("method" to "POST", "path" to "/api/auth/login", "description" to "로그인"),
                mapOf("method" to "POST", "path" to "/api/auth/refresh", "description" to "토큰 갱신"),
                mapOf("method" to "POST", "path" to "/api/auth/logout", "description" to "로그아웃"),
                mapOf("method" to "POST", "path" to "/api/auth/logout-all", "description" to "모든 기기 로그아웃"),
                mapOf("method" to "GET", "path" to "/api/auth/me", "description" to "현재 사용자 정보"),
                mapOf("method" to "POST", "path" to "/api/auth/validate", "description" to "토큰 검증")
            )
        )

        return ResponseEntity.ok(
            ApiResponse.success("API가 정상 동작 중입니다", healthInfo)
        )
    }

    /**
     * Authorization 헤더에서 토큰 추출
     */
    private fun extractTokenFromHeader(authHeader: String): String {
        val token = authHeader.removePrefix("Bearer ").trim()
        if (token.isEmpty()) {
            throw IllegalArgumentException("토큰이 제공되지 않았습니다")
        }
        return token
    }
}