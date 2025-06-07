package com.example.kopringCRUD.presentation.user

import com.example.kopringCRUD.domain.user.dto.*
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.domain.user.service.UserService
import com.example.kopringCRUD.global.common.PageRequest
import com.example.kopringCRUD.global.common.PageResponse
import com.example.kopringCRUD.global.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 사용자 관리 REST API 컨트롤러
 * 사용자 프로필 조회/수정, 계정 관리 기능 제공
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["*"])
class UserController(
    private val userService: UserService
) {

    // ========================================
    // 내 정보 관리 (인증된 사용자)
    // ========================================

    /**
     * 내 정보 조회
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @return 내 사용자 정보
     */
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<UserResponse> {
        val user = userService.findById(userPrincipal.id)
        return ResponseEntity.ok(user)
    }

    /**
     * 내 정보 수정
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param request 수정할 정보 (표시명, 아바타 URL)
     * @return 수정된 사용자 정보
     */
    @PutMapping("/me")
    fun updateCurrentUser(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateUser(userPrincipal.id, request)
        return ResponseEntity.ok(updatedUser)
    }

    /**
     * 비밀번호 변경
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @param request 비밀번호 변경 정보 (현재 비밀번호, 새 비밀번호)
     * @return 수정된 사용자 정보
     */
    @PutMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.changePassword(userPrincipal.id, request)
        return ResponseEntity.ok(updatedUser)
    }

    /**
     * 계정 비활성화 (회원 탈퇴)
     *
     * @param userPrincipal 현재 인증된 사용자 정보
     * @return 비활성화된 사용자 정보
     */
    @DeleteMapping("/me")
    fun deactivateCurrentUser(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<UserResponse> {
        val deactivatedUser = userService.deactivateUser(userPrincipal.id)
        return ResponseEntity.ok(deactivatedUser)
    }

    // ========================================
    // 다른 사용자 정보 조회 (공개 정보)
    // ========================================

    /**
     * 특정 사용자 정보 조회 (공개 정보만)
     *
     * @param id 조회할 사용자 ID
     * @return 사용자 공개 정보
     */
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.findById(id)
        return ResponseEntity.ok(user)
    }

    /**
     * 사용자명으로 사용자 조회
     *
     * @param username 조회할 사용자명
     * @return 사용자 정보
     */
    @GetMapping("/username/{username}")
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<UserResponse> {
        val user = userService.findByUsername(username)
        return ResponseEntity.ok(user)
    }

    /**
     * 활성 사용자 목록 조회 (페이징)
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param search 검색 키워드 (사용자명 검색, 선택적)
     * @return 활성 사용자 목록
     */
    @GetMapping
    fun getActiveUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<PageResponse<UserResponse>> {
        val pageRequest = PageRequest(page, size)

        val users = if (search != null) {
            userService.searchByUsername(search, pageRequest)
        } else {
            userService.findActiveUsers(pageRequest)
        }

        return ResponseEntity.ok(users)
    }

    // ========================================
    // 관리자 전용 기능
    // ========================================

    /**
     * 모든 사용자 조회 (관리자 전용)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param role 역할별 필터링 (선택적)
     * @param search 검색 키워드 (선택적)
     * @return 사용자 목록
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<PageResponse<UserResponse>> {
        val pageRequest = PageRequest(page, size)

        val users = when {
            search != null -> userService.searchByUsername(search, pageRequest)
            role != null -> userService.findByRole(role, pageRequest)
            else -> userService.findAllUsers(pageRequest)
        }

        return ResponseEntity.ok(users)
    }

    /**
     * 사용자 역할 변경 (관리자 전용)
     *
     * @param id 대상 사용자 ID
     * @param request 변경할 역할 정보
     * @return 수정된 사용자 정보
     */
    @PutMapping("/admin/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    fun changeUserRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangeUserRoleRequest
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.changeUserRole(id, request)
        return ResponseEntity.ok(updatedUser)
    }

    /**
     * 사용자 계정 활성화 (관리자 전용)
     *
     * @param id 대상 사용자 ID
     * @return 활성화된 사용자 정보
     */
    @PutMapping("/admin/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    fun activateUser(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val activatedUser = userService.activateUser(id)
        return ResponseEntity.ok(activatedUser)
    }

    /**
     * 사용자 계정 비활성화 (관리자 전용)
     *
     * @param id 대상 사용자 ID
     * @return 비활성화된 사용자 정보
     */
    @PutMapping("/admin/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    fun deactivateUser(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val deactivatedUser = userService.deactivateUser(id)
        return ResponseEntity.ok(deactivatedUser)
    }

    /**
     * 사용자 강제 삭제 (관리자 전용, 신중히 사용)
     *
     * @param id 삭제할 사용자 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        userService.deleteUser(id)
        return ResponseEntity.ok(
            mapOf(
                "message" to "사용자가 성공적으로 삭제되었습니다",
                "deletedUserId" to id.toString()
            )
        )
    }

    // ========================================
    // 통계 및 유틸리티
    // ========================================

    /**
     * 사용자 통계 조회 (관리자/모더레이터)
     *
     * @return 사용자 관련 통계 정보
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getUserStatistics(): ResponseEntity<Map<String, Any>> {
        // 실제 구현에서는 UserService에 통계 메소드 추가 필요
        val statistics = mapOf(
            "totalUsers" to "통계 구현 필요",
            "activeUsers" to "통계 구현 필요",
            "adminUsers" to "통계 구현 필요",
            "moderatorUsers" to "통계 구현 필요",
            "newUsersThisMonth" to "통계 구현 필요"
        )

        return ResponseEntity.ok(statistics)
    }

    /**
     * 사용자명/이메일 중복 확인
     *
     * @param username 확인할 사용자명 (선택적)
     * @param email 확인할 이메일 (선택적)
     * @return 중복 확인 결과
     */
    @GetMapping("/check-duplicate")
    fun checkDuplicate(
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false) email: String?
    ): ResponseEntity<Map<String, Any>> {
        val result = mutableMapOf<String, Any>()

        if (username != null) {
            // UserRepository의 existsByUsername 사용
            // 현재는 UserService에 해당 메소드가 없으므로 임시 구현
            result["usernameExists"] = false  // 실제로는 userService.existsByUsername(username)
            result["usernameMessage"] = if (result["usernameExists"] as Boolean) {
                "이미 사용중인 사용자명입니다"
            } else {
                "사용 가능한 사용자명입니다"
            }
        }

        if (email != null) {
            result["emailExists"] = false  // 실제로는 userService.existsByEmail(email)
            result["emailMessage"] = if (result["emailExists"] as Boolean) {
                "이미 사용중인 이메일입니다"
            } else {
                "사용 가능한 이메일입니다"
            }
        }

        if (username == null && email == null) {
            result["error"] = "확인할 사용자명 또는 이메일을 제공해주세요"
            return ResponseEntity.badRequest().body(result)
        }

        return ResponseEntity.ok(result)
    }

    /**
     * 내 활동 요약 정보
     *
     * @param userPrincipal 현재 인증된 사용자
     * @return 사용자 활동 요약 (채팅방, 메시지, 신고 등)
     */
    @GetMapping("/me/activity")
    fun getMyActivity(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<Map<String, Any>> {
        // 실제 구현에서는 ChatService, ReportService와 연동 필요
        val activity = mapOf(
            "userId" to userPrincipal.id,
            "createdChatRooms" to "구현 필요", // chatService.countByCreatorId(userPrincipal.id)
            "sentMessages" to "구현 필요",     // chatService.countMessagesBySenderId(userPrincipal.id)
            "submittedReports" to "구현 필요", // reportService.countByReporterId(userPrincipal.id)
            "receivedReports" to "구현 필요",  // reportService.countByReportedUserId(userPrincipal.id)
            "joinDate" to "구현 필요",         // 사용자 생성일
            "lastActive" to "구현 필요"        // 마지막 활동 시간
        )

        return ResponseEntity.ok(activity)
    }

    /**
     * API 상태 확인 (헬스체크)
     *
     * @return API 상태 정보
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "User Management API",
                "timestamp" to System.currentTimeMillis(),
                "endpoints" to listOf(
                    mapOf("method" to "GET", "path" to "/api/users/me", "description" to "내 정보 조회"),
                    mapOf("method" to "PUT", "path" to "/api/users/me", "description" to "내 정보 수정"),
                    mapOf("method" to "PUT", "path" to "/api/users/me/password", "description" to "비밀번호 변경"),
                    mapOf("method" to "DELETE", "path" to "/api/users/me", "description" to "계정 비활성화"),
                    mapOf("method" to "GET", "path" to "/api/users/{id}", "description" to "사용자 정보 조회"),
                    mapOf("method" to "GET", "path" to "/api/users", "description" to "사용자 목록 조회"),
                    mapOf("method" to "GET", "path" to "/api/users/admin/all", "description" to "전체 사용자 조회 (관리자)"),
                    mapOf("method" to "PUT", "path" to "/api/users/admin/{id}/role", "description" to "역할 변경 (관리자)")
                )
            )
        )
    }
}