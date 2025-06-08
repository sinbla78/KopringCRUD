package com.example.kopringCRUD.domain.user.repository

import com.example.kopringCRUD.domain.user.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    /**
     * 토큰으로 RefreshToken 조회
     */
    fun findByToken(token: String): RefreshToken?

    /**
     * 사용자 ID로 모든 RefreshToken 조회
     */
    fun findByUserId(userId: Long): List<RefreshToken>

    /**
     * 사용자 ID로 유효한 RefreshToken만 조회
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.expiryDate > :now")
    fun findValidTokensByUserId(@Param("userId") userId: Long, @Param("now") now: LocalDateTime = LocalDateTime.now()): List<RefreshToken>

    /**
     * 사용자의 모든 RefreshToken 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)

    /**
     * 특정 토큰 삭제
     */
    fun deleteByToken(token: String)

    /**
     * 만료된 토큰들 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    fun deleteByExpiryDateBefore(@Param("now") now: LocalDateTime = LocalDateTime.now())

    /**
     * 만료된 토큰들 삭제 (편의 메서드)
     * 직접 @Query 어노테이션 사용
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    fun deleteExpiredTokens()

    /**
     * 사용자의 활성 토큰 개수 조회
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.expiryDate > :now")
    fun countActiveTokensByUserId(@Param("userId") userId: Long, @Param("now") now: LocalDateTime = LocalDateTime.now()): Long

    /**
     * 사용자별 토큰 개수 제한을 위한 오래된 토큰 삭제
     * (한 사용자당 최대 N개의 토큰만 유지)
     */
    @Modifying
    @Query("""
        DELETE FROM RefreshToken rt 
        WHERE rt.userId = :userId 
        AND rt.id NOT IN (
            SELECT rt2.id FROM RefreshToken rt2 
            WHERE rt2.userId = :userId 
            ORDER BY rt2.createdAt DESC 
            LIMIT :maxTokens
        )
    """)
    fun deleteOldTokensForUser(@Param("userId") userId: Long, @Param("maxTokens") maxTokens: Int)

    /**
     * 특정 IP 주소로 생성된 토큰들 조회 (보안 모니터링용)
     */
    fun findByIpAddress(ipAddress: String): List<RefreshToken>

    /**
     * 특정 기간 내에 생성된 토큰들 조회
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(@Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime): List<RefreshToken>

    /**
     * 사용자의 가장 최근 토큰 조회
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId ORDER BY rt.createdAt DESC LIMIT 1")
    fun findLatestTokenByUserId(@Param("userId") userId: Long): RefreshToken?

    /**
     * 곧 만료될 토큰들 조회 (알림용)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiryDate BETWEEN :now AND :soonExpiry")
    fun findTokensExpiringSoon(@Param("now") now: LocalDateTime, @Param("soonExpiry") soonExpiry: LocalDateTime): List<RefreshToken>

    /**
     * 토큰 존재 여부 확인
     */
    fun existsByToken(token: String): Boolean

    /**
     * 사용자가 유효한 토큰을 가지고 있는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt WHERE rt.userId = :userId AND rt.expiryDate > :now")
    fun hasValidTokens(@Param("userId") userId: Long, @Param("now") now: LocalDateTime = LocalDateTime.now()): Boolean
}