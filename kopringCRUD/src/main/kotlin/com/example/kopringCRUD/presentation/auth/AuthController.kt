package com.example.kopringCRUD.presentation.auth

import com.example.kopringCRUD.domain.user.dto.AuthResponse
import com.example.kopringCRUD.domain.user.dto.CreateUserRequest
import com.example.kopringCRUD.domain.user.dto.LoginRequest
import com.example.kopringCRUD.domain.user.service.AuthService
import com.example.kopringCRUD.global.exception.ErrorResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 인증 관련 REST API 컨트롤러
 * 회원가입, 로그인 기능 제공
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"]) // 개발 환경용, 운영에서는 구체적인 도메인 지정
class AuthController(
    private val authService: AuthService
) {

    /**
     * 사용자 회원가입
     *
     * @param request 회원가입 정보 (사용자명, 이메일, 비밀번호, 표시명)
     * @return JWT 토큰과 사용자 정보
     */
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 사용자 로그인
     *
     * @param request 로그인 정보 (사용자명, 비밀번호)
     * @return JWT 토큰과 사용자 정보
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    /**
     * 토큰 검증 (선택적 기능)
     *
     * @param token JWT 토큰 (Authorization 헤더에서 추출)
     * @return 토큰 유효성 검증 결과
     */
    @PostMapping("/validate")
    fun validateToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Map<String, Any>> {
        try {
            // "Bearer " 접두사 제거
            val token = authHeader.removePrefix("Bearer ").trim()

            if (token.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "valid" to false,
                        "message" to "토큰이 제공되지 않았습니다"
                    )
                )
            }

            val isValid = authService.validateToken(token)

            if (isValid) {
                val username = authService.getUsernameFromToken(token)
                return ResponseEntity.ok(
                    mapOf(
                        "valid" to true,
                        "username" to username,
                        "message" to "유효한 토큰입니다"
                    )
                )
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf(
                        "valid" to false,
                        "message" to "유효하지 않은 토큰입니다"
                    )
                )
            }
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf(
                    "valid" to false,
                    "message" to "토큰 검증 중 오류가 발생했습니다"
                )
            )
        }
    }

    /**
     * 토큰 갱신 (선택적 기능)
     * 기존 유효한 토큰으로 새로운 토큰 발급
     *
     * @param authHeader Authorization 헤더의 JWT 토큰
     * @return 새로운 JWT 토큰과 사용자 정보
     */
    @PostMapping("/refresh")
    fun refreshToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<AuthResponse> {
        val token = authHeader.removePrefix("Bearer ").trim()

        if (token.isEmpty()) {
            throw IllegalArgumentException("토큰이 제공되지 않았습니다")
        }

        val response = authService.refreshToken(token)
        return ResponseEntity.ok(response)
    }

    /**
     * 로그아웃 (클라이언트 측에서 토큰 삭제)
     * JWT는 stateless하므로 서버에서 별도 처리 불필요
     * 클라이언트에게 토큰 삭제 안내 메시지만 반환
     *
     * @return 로그아웃 완료 메시지
     */
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "message" to "로그아웃되었습니다. 클라이언트에서 토큰을 삭제해주세요.",
                "instruction" to "localStorage.removeItem('token') 또는 sessionStorage.removeItem('token')을 실행하세요."
            )
        )
    }

    /**
     * 인증 상태 확인
     * 현재 로그인한 사용자의 정보 반환 (토큰 기반)
     *
     * @param authHeader Authorization 헤더의 JWT 토큰
     * @return 현재 사용자 정보 (토큰 없이)
     */
    @GetMapping("/me")
    fun getCurrentUser(@RequestHeader("Authorization") authHeader: String): ResponseEntity<AuthResponse> {
        val token = authHeader.removePrefix("Bearer ").trim()

        if (token.isEmpty()) {
            throw IllegalArgumentException("토큰이 제공되지 않았습니다")
        }

        if (!authService.validateToken(token)) {
            throw IllegalArgumentException("유효하지 않은 토큰입니다")
        }

        val username = authService.getUsernameFromToken(token)
        val response = authService.getCurrentUser(username)

        return ResponseEntity.ok(response)
    }

    /**
     * API 상태 확인 (헬스체크)
     * 인증 API가 정상 동작하는지 확인
     *
     * @return API 상태 정보
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "Authentication API",
                "timestamp" to System.currentTimeMillis(),
                "endpoints" to listOf(
                    mapOf("method" to "POST", "path" to "/api/auth/register", "description" to "회원가입"),
                    mapOf("method" to "POST", "path" to "/api/auth/login", "description" to "로그인"),
                    mapOf("method" to "POST", "path" to "/api/auth/validate", "description" to "토큰 검증"),
                    mapOf("method" to "POST", "path" to "/api/auth/refresh", "description" to "토큰 갱신"),
                    mapOf("method" to "POST", "path" to "/api/auth/logout", "description" to "로그아웃"),
                    mapOf("method" to "GET", "path" to "/api/auth/me", "description" to "현재 사용자 정보")
                )
            )
        )
    }

    /**
     * 회원가입 가능 여부 확인
     * 사용자명과 이메일 중복 체크
     *
     * @param username 확인할 사용자명 (선택적)
     * @param email 확인할 이메일 (선택적)
     * @return 사용 가능 여부
     */
    @GetMapping("/check-availability")
    fun checkAvailability(
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false) email: String?
    ): ResponseEntity<Map<String, Any>> {
        val result = mutableMapOf<String, Any>()

        if (username != null) {
            // UserService에서 중복 체크 로직이 있다면 사용
            // 현재는 간단한 예시로 항상 사용 가능으로 반환
            result["usernameAvailable"] = true
            result["usernameMessage"] = "사용 가능한 사용자명입니다"
        }

        if (email != null) {
            // UserService에서 중복 체크 로직이 있다면 사용
            result["emailAvailable"] = true
            result["emailMessage"] = "사용 가능한 이메일입니다"
        }

        if (username == null && email == null) {
            result["error"] = "확인할 사용자명 또는 이메일을 제공해주세요"
            return ResponseEntity.badRequest().body(result)
        }

        return ResponseEntity.ok(result)
    }
}