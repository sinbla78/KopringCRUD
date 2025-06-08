package com.example.kopringCRUD.domain.user.service

import com.example.kopringCRUD.domain.user.dto.AuthResponse
import com.example.kopringCRUD.domain.user.dto.CreateUserRequest
import com.example.kopringCRUD.domain.user.dto.LoginRequest
import com.example.kopringCRUD.domain.user.dto.RefreshTokenRequest
import com.example.kopringCRUD.domain.user.entity.RefreshToken
import com.example.kopringCRUD.domain.user.repository.RefreshTokenRepository
import com.example.kopringCRUD.global.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class AuthService(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    /**
     * 사용자 회원가입
     * 1. 사용자 생성
     * 2. 자동 로그인 처리
     * 3. JWT 토큰 및 Refresh 토큰 발급
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
            val accessToken = jwtTokenProvider.generateAccessToken(authentication)
            val refreshToken = generateRefreshToken(user.id!!)

            return AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken.token,
                user = user
            )

        } catch (e: AuthenticationException) {
            throw RuntimeException("회원가입은 완료되었지만 자동 로그인에 실패했습니다. 수동으로 로그인해 주세요.", e)
        }
    }

    /**
     * 사용자 로그인
     * 1. 사용자 인증
     * 2. JWT 토큰 및 Refresh 토큰 발급
     */
    fun login(request: LoginRequest): AuthResponse {
        // 1. 사용자 인증
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        // 2. 사용자 정보 조회
        val user = userService.findByUsername(request.username)

        // 3. 기존 refresh token 무효화 (선택적)
        refreshTokenRepository.deleteByUserId(user.id!!)

        // 4. 새로운 토큰들 생성
        val accessToken = jwtTokenProvider.generateAccessToken(authentication)
        val refreshToken = generateRefreshToken(user.id!!)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            user = user
        )
    }

    /**
     * Access Token 갱신
     * Refresh Token을 사용하여 새로운 Access Token 발급
     */
    fun refreshAccessToken(request: RefreshTokenRequest): AuthResponse {
        val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw RuntimeException("유효하지 않은 Refresh Token입니다")

        // 1. Refresh Token 만료 확인
        if (refreshTokenEntity.expiryDate.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshTokenEntity)
            throw RuntimeException("Refresh Token이 만료되었습니다")
        }

        // 2. 사용자 정보 조회
        val user = userService.findById(refreshTokenEntity.userId)

        // 3. UserDetails 객체 생성하여 Authentication 생성
        val userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.username)  // user.profile.username 대신 user.username 사용
            .password("") // 비밀번호는 빈 문자열로 (토큰 갱신시에는 필요없음)
            .authorities("ROLE_${user.role.name}")
            .build()

        val authentication = UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.authorities
        )

        val newAccessToken = jwtTokenProvider.generateAccessToken(authentication)

        // 4. Refresh Token 갱신 (선택적 - 보안 강화)
        val newRefreshToken = rotateRefreshToken(refreshTokenEntity)

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken.token,
            user = user
        )
    }

    /**
     * 로그아웃
     * Refresh Token 무효화
     */
    fun logout(refreshToken: String) {
        refreshTokenRepository.findByToken(refreshToken)?.let {
            refreshTokenRepository.delete(it)
        }
    }

    /**
     * 모든 기기에서 로그아웃
     * 사용자의 모든 Refresh Token 무효화
     */
    fun logoutFromAllDevices(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    /**
     * Access Token 검증
     */
    @Transactional(readOnly = true)
    fun validateAccessToken(token: String): Boolean {
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
        return AuthResponse(
            accessToken = "",
            refreshToken = "",
            user = user
        )
    }

    /**
     * Refresh Token 생성
     */
    private fun generateRefreshToken(userId: Long): RefreshToken {
        val token = UUID.randomUUID().toString()
        val expiryDate = LocalDateTime.now().plusDays(7) // 7일 후 만료

        val refreshToken = RefreshToken(
            token = token,
            userId = userId,
            expiryDate = expiryDate
        )

        return refreshTokenRepository.save(refreshToken)
    }

    /**
     * Refresh Token 회전 (보안 강화)
     * 기존 토큰을 삭제하고 새로운 토큰 생성
     */
    private fun rotateRefreshToken(oldRefreshToken: RefreshToken): RefreshToken {
        // 기존 토큰 삭제
        refreshTokenRepository.delete(oldRefreshToken)

        // 새로운 토큰 생성
        return generateRefreshToken(oldRefreshToken.userId)
    }

    /**
     * 만료된 Refresh Token 정리 (스케줄러에서 호출)
     */
    @Transactional
    fun cleanupExpiredRefreshTokens() {
        refreshTokenRepository.deleteExpiredTokens()
    }
}