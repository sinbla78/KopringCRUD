package com.example.kopringCRUD.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.security.Key
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret:mySecretKeyForJWTTokenMustBeLongEnoughForHS512Algorithm}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.access-token.expiration:900000}") // 15분 (15 * 60 * 1000)
    private val accessTokenExpiration: Long = 900000

    @Value("\${jwt.refresh-token.expiration:604800000}") // 7일 (7 * 24 * 60 * 60 * 1000)
    private val refreshTokenExpiration: Long = 604800000

    @Value("\${jwt.issuer:kopring-crud}")
    private lateinit var issuer: String

    private val key: Key by lazy { Keys.hmacShaKeyFor(jwtSecret.toByteArray()) }

    /**
     * Access Token 생성
     * 짧은 만료시간을 가진 토큰 (기본 15분)
     */
    fun generateAccessToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as org.springframework.security.core.userdetails.UserDetails
        val now = Date()
        val expiryDate = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            .setSubject(userPrincipal.username)
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "access")
            .claim("authorities", userPrincipal.authorities.map { it.authority })
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * 사용자명으로 Access Token 생성 (Refresh Token 갱신 시 사용)
     */
    fun generateAccessToken(username: String, authorities: List<String> = emptyList()): String {
        val now = Date()
        val expiryDate = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            .setSubject(username)
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "access")
            .claim("authorities", authorities)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * 토큰에서 사용자명 추출
     */
    fun getUsernameFromToken(token: String): String {
        val claims: Claims = getClaimsFromToken(token)
        return claims.subject
    }

    /**
     * 토큰에서 권한 정보 추출
     */
    fun getAuthoritiesFromToken(token: String): List<String> {
        val claims: Claims = getClaimsFromToken(token)
        return claims.get("authorities", List::class.java) as? List<String> ?: emptyList()
    }

    /**
     * 토큰 타입 확인 (access/refresh)
     */
    fun getTokenType(token: String): String? {
        val claims: Claims = getClaimsFromToken(token)
        return claims.get("type", String::class.java)
    }

    /**
     * 토큰 만료시간 조회
     */
    fun getExpirationDateFromToken(token: String): Date {
        val claims: Claims = getClaimsFromToken(token)
        return claims.expiration
    }

    /**
     * 토큰이 곧 만료되는지 확인 (예: 5분 이내)
     */
    fun isTokenExpiringSoon(token: String, minutesBefore: Long = 5): Boolean {
        val expirationDate = getExpirationDateFromToken(token)
        val thresholdTime = Date(System.currentTimeMillis() + (minutesBefore * 60 * 1000))
        return expirationDate.before(thresholdTime)
    }

    /**
     * 토큰의 남은 유효시간 (초 단위)
     */
    fun getRemainingValidityInSeconds(token: String): Long {
        val expirationDate = getExpirationDateFromToken(token)
        val now = Date()
        return if (expirationDate.after(now)) {
            (expirationDate.time - now.time) / 1000
        } else {
            0L
        }
    }

    /**
     * Access Token 검증
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            val tokenType = claims.get("type", String::class.java)

            // Access Token인지 확인
            tokenType == "access" && !isTokenExpired(token)
        } catch (ex: Exception) {
            false
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    fun isTokenExpired(token: String): Boolean {
        val expirationDate = getExpirationDateFromToken(token)
        return expirationDate.before(Date())
    }

    /**
     * 토큰이 유효한 형식인지 확인 (만료 여부는 별도)
     */
    fun isTokenValid(token: String): Boolean {
        return try {
            getClaimsFromToken(token)
            true
        } catch (ex: Exception) {
            false
        }
    }

    /**
     * Access Token의 만료시간 (초 단위) 반환
     */
    fun getAccessTokenExpirationInSeconds(): Long {
        return accessTokenExpiration / 1000
    }

    /**
     * Refresh Token의 만료시간 (초 단위) 반환
     */
    fun getRefreshTokenExpirationInSeconds(): Long {
        return refreshTokenExpiration / 1000
    }

    /**
     * 토큰에서 Claims 추출
     */
    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    /**
     * 토큰 생성 시간 조회
     */
    fun getIssuedAtFromToken(token: String): Date {
        val claims: Claims = getClaimsFromToken(token)
        return claims.issuedAt
    }

    /**
     * 토큰이 특정 시간 이후에 생성되었는지 확인
     */
    fun isTokenIssuedAfter(token: String, timestamp: LocalDateTime): Boolean {
        val issuedAt = getIssuedAtFromToken(token)
        val issuedAtLocalDateTime = issuedAt.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return issuedAtLocalDateTime.isAfter(timestamp)
    }

    /**
     * 디버깅용 토큰 정보 출력
     */
    fun getTokenInfo(token: String): Map<String, Any> {
        return try {
            val claims = getClaimsFromToken(token)
            mapOf(
                "subject" to claims.subject,
                "issuer" to (claims.issuer ?: ""),
                "issuedAt" to claims.issuedAt,
                "expiration" to claims.expiration,
                "type" to (claims.get("type", String::class.java) ?: "unknown"),
                "authorities" to (claims.get("authorities", List::class.java) ?: emptyList<String>()),
                "isExpired" to isTokenExpired(token),
                "remainingSeconds" to getRemainingValidityInSeconds(token)
            )
        } catch (ex: Exception) {
            mapOf("error" to ex.message.orEmpty())
        }
    }

    // 기존 메서드와의 호환성을 위해 유지 (Deprecated)
    @Deprecated("Use generateAccessToken instead", ReplaceWith("generateAccessToken(authentication)"))
    fun generateToken(authentication: Authentication): String {
        return generateAccessToken(authentication)
    }
}