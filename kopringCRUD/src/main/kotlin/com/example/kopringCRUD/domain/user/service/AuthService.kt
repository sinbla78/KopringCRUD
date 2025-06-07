package com.example.kopringCRUD.domain.user.service


import com.example.kopringCRUD.domain.user.dto.AuthResponse
import com.example.kopringCRUD.domain.user.dto.CreateUserRequest
import com.example.kopringCRUD.domain.user.dto.LoginRequest
import com.example.kopringCRUD.global.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * 사용자 회원가입
     * 1. 사용자 생성
     * 2. 자동 로그인 처리
     * 3. JWT 토큰 발급
     */
    fun register(request: CreateUserRequest): AuthResponse {
        // 1. 사용자 생성
        val user = userService.createUser(request)

        try {
            // 2. 자동 로그인 처리
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, request.password)
            )

            // 3. JWT 토큰 생성
            val token = jwtTokenProvider.generateToken(authentication)

            return AuthResponse(token = token, user = user)

        } catch (e: AuthenticationException) {
            // 회원가입은 성공했지만 로그인에 실패한 경우
            // 이론적으로는 발생하지 않아야 하지만, 안전을 위해 예외 처리
            throw RuntimeException("회원가입은 완료되었지만 자동 로그인에 실패했습니다. 수동으로 로그인해 주세요.", e)
        }
    }

    /**
     * 사용자 로그인
     * 1. 사용자 인증
     * 2. JWT 토큰 발급
     */
    fun login(request: LoginRequest): AuthResponse {
        // 1. 사용자 인증 (Spring Security)
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        // 2. JWT 토큰 생성
        val token = jwtTokenProvider.generateToken(authentication)

        // 3. 사용자 정보 조회
        val user = userService.findByUsername(request.username)

        return AuthResponse(token = token, user = user)
    }

    /**
     * 토큰 검증
     */
    @Transactional(readOnly = true)
    fun validateToken(token: String): Boolean {
        return jwtTokenProvider.validateToken(token)
    }

    /**
     * 토큰에서 사용자명 추출
     */
    @Transactional(readOnly = true)
    fun getUsernameFromToken(token: String): String {
        return jwtTokenProvider.getUsernameFromToken(token)
    }

    /**
     * 현재 인증된 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    fun getCurrentUser(username: String): AuthResponse {
        val user = userService.findByUsername(username)

        // 토큰은 이미 유효하므로 새로 생성하지 않고 빈 문자열 반환
        // 실제로는 현재 토큰을 전달받아 반환해야 하지만,
        // 이 메소드는 주로 사용자 정보 확인용이므로 토큰은 생략
        return AuthResponse(token = "", user = user)
    }

    /**
     * 토큰 갱신 (선택적 기능)
     * 기존 토큰이 유효한 경우 새로운 토큰 발급
     */
    fun refreshToken(oldToken: String): AuthResponse {
        if (!jwtTokenProvider.validateToken(oldToken)) {
            throw RuntimeException("유효하지 않은 토큰입니다")
        }

        val username = jwtTokenProvider.getUsernameFromToken(oldToken)
        val user = userService.findByUsername(username)

        // 새 토큰 생성을 위해 다시 인증 (실제로는 더 효율적인 방법 사용 가능)
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(username, null)
        )

        val newToken = jwtTokenProvider.generateToken(authentication)

        return AuthResponse(token = newToken, user = user)
    }
}