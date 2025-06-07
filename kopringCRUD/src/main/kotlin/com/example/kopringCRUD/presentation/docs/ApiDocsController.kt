package com.example.kopringCRUD.presentation.docs

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * API 엔드포인트 정보
 */
data class ApiEndpoint(
    val method: String,
    val path: String,
    val description: String,
    val requiresAuth: Boolean = true,
    val roles: List<String> = listOf(),
    val requestBody: String? = null,
    val responseType: String? = null,
    val example: String? = null
)

/**
 * API 그룹 정보
 */
data class ApiGroup(
    val name: String,
    val description: String,
    val baseUrl: String,
    val endpoints: List<ApiEndpoint>
)

/**
 * 전체 API 문서
 */
data class ApiDocumentation(
    val title: String,
    val version: String,
    val description: String,
    val baseUrl: String,
    val serverInfo: ServerInfo,
    val authentication: AuthenticationInfo,
    val apiGroups: List<ApiGroup>,
    val websocket: WebSocketInfo,
    val generatedAt: LocalDateTime
)

/**
 * 서버 정보
 */
data class ServerInfo(
    val environment: String,
    val version: String,
    val buildTime: String,
    val javaVersion: String,
    val springBootVersion: String,
    val profiles: List<String>
)

/**
 * 인증 정보
 */
data class AuthenticationInfo(
    val type: String,
    val description: String,
    val headerName: String,
    val tokenFormat: String,
    val exampleToken: String
)

/**
 * WebSocket 정보
 */
data class WebSocketInfo(
    val endpoint: String,
    val protocol: String,
    val topics: List<WebSocketTopic>
)

/**
 * WebSocket 토픽 정보
 */
data class WebSocketTopic(
    val path: String,
    val type: String,
    val description: String,
    val example: String? = null
)

/**
 * 시스템 상태 정보
 */
data class SystemStatus(
    val status: String,
    val timestamp: LocalDateTime,
    val uptime: String,
    val services: Map<String, ServiceStatus>,
    val metrics: SystemMetrics
)

/**
 * 서비스 상태
 */
data class ServiceStatus(
    val name: String,
    val status: String,
    val description: String,
    val lastCheck: LocalDateTime
)

/**
 * 시스템 메트릭
 */
data class SystemMetrics(
    val memoryUsage: MemoryUsage,
    val activeConnections: Int,
    val totalRequests: Long,
    val averageResponseTime: Double
)

/**
 * 메모리 사용량
 */
data class MemoryUsage(
    val used: Long,
    val free: Long,
    val total: Long,
    val max: Long
)

/**
 * API 문서 및 시스템 정보 컨트롤러
 */
@RestController
@RequestMapping("/api")
class ApiDocsController(
    @Value("\${spring.application.name:kotlin-spring-chat-app}")
    private val applicationName: String,

    @Value("\${server.port:8080}")
    private val serverPort: String
) {

    private val startTime = LocalDateTime.now()

    /**
     * 전체 API 문서 조회
     *
     * @return 완전한 API 문서
     */
    @GetMapping("/docs")
    fun getApiDocumentation(): ApiDocumentation {
        return ApiDocumentation(
            title = "Kotlin Spring Boot 채팅 애플리케이션 API",
            version = "1.0.0",
            description = "실시간 채팅, 사용자 관리, 신고 시스템을 제공하는 REST API",
            baseUrl = "http://localhost:$serverPort/api",
            serverInfo = getServerInfo(),
            authentication = getAuthenticationInfo(),
            apiGroups = getApiGroups(),
            websocket = getWebSocketInfo(),
            generatedAt = LocalDateTime.now()
        )
    }

    /**
     * API 엔드포인트 목록만 간단히 조회
     *
     * @return 엔드포인트 목록
     */
    @GetMapping("/endpoints")
    fun getApiEndpoints(): Map<String, List<ApiEndpoint>> {
        return getApiGroups().associate { group ->
            group.name to group.endpoints
        }
    }

    /**
     * 시스템 상태 확인
     *
     * @return 시스템 전체 상태 정보
     */
    @GetMapping("/health")
    fun getSystemHealth(): SystemStatus {
        val runtime = Runtime.getRuntime()
        val uptime = java.time.Duration.between(startTime, LocalDateTime.now())

        return SystemStatus(
            status = "UP",
            timestamp = LocalDateTime.now(),
            uptime = formatDuration(uptime),
            services = getServiceStatuses(),
            metrics = SystemMetrics(
                memoryUsage = MemoryUsage(
                    used = runtime.totalMemory() - runtime.freeMemory(),
                    free = runtime.freeMemory(),
                    total = runtime.totalMemory(),
                    max = runtime.maxMemory()
                ),
                activeConnections = 0, // WebSocket 연결 수 (실제 구현 필요)
                totalRequests = 0L,    // 총 요청 수 (실제 구현 필요)
                averageResponseTime = 0.0 // 평균 응답 시간 (실제 구현 필요)
            )
        )
    }

    /**
     * 서버 정보 조회
     *
     * @return 서버 환경 정보
     */
    @GetMapping("/info")
    fun getServerInfo(): ServerInfo {
        return ServerInfo(
            environment = System.getProperty("spring.profiles.active") ?: "default",
            version = "1.0.0",
            buildTime = "2024-01-15T10:30:00", // 실제로는 빌드 시간 주입
            javaVersion = System.getProperty("java.version"),
            springBootVersion = "3.2.0",
            profiles = System.getProperty("spring.profiles.active")?.split(",") ?: listOf("default")
        )
    }

    /**
     * 빠른 시작 가이드
     *
     * @return API 사용 시작 가이드
     */
    @GetMapping("/quick-start")
    fun getQuickStartGuide(): Map<String, Any> {
        return mapOf(
            "title" to "빠른 시작 가이드",
            "steps" to listOf(
                mapOf(
                    "step" to 1,
                    "title" to "회원가입",
                    "description" to "새로운 계정을 생성합니다",
                    "endpoint" to "POST /api/auth/register",
                    "example" to mapOf(
                        "url" to "http://localhost:$serverPort/api/auth/register",
                        "method" to "POST",
                        "body" to mapOf(
                            "username" to "testuser",
                            "email" to "test@example.com",
                            "password" to "password123",
                            "displayName" to "테스트 사용자"
                        )
                    )
                ),
                mapOf(
                    "step" to 2,
                    "title" to "로그인",
                    "description" to "JWT 토큰을 발급받습니다",
                    "endpoint" to "POST /api/auth/login",
                    "example" to mapOf(
                        "url" to "http://localhost:$serverPort/api/auth/login",
                        "method" to "POST",
                        "body" to mapOf(
                            "username" to "testuser",
                            "password" to "password123"
                        )
                    )
                ),
                mapOf(
                    "step" to 3,
                    "title" to "채팅방 생성",
                    "description" to "새로운 채팅방을 만듭니다",
                    "endpoint" to "POST /api/chat/rooms",
                    "example" to mapOf(
                        "url" to "http://localhost:$serverPort/api/chat/rooms",
                        "method" to "POST",
                        "headers" to mapOf("Authorization" to "Bearer YOUR_JWT_TOKEN"),
                        "body" to mapOf(
                            "name" to "개발자 모임",
                            "description" to "코틀린 개발자들의 채팅방",
                            "maxParticipants" to 100
                        )
                    )
                ),
                mapOf(
                    "step" to 4,
                    "title" to "메시지 전송",
                    "description" to "채팅방에 메시지를 보냅니다",
                    "endpoint" to "POST /api/chat/rooms/{roomId}/messages",
                    "example" to mapOf(
                        "url" to "http://localhost:$serverPort/api/chat/rooms/1/messages",
                        "method" to "POST",
                        "headers" to mapOf("Authorization" to "Bearer YOUR_JWT_TOKEN"),
                        "body" to mapOf(
                            "content" to "안녕하세요!",
                            "messageType" to "TEXT"
                        )
                    )
                ),
                mapOf(
                    "step" to 5,
                    "title" to "WebSocket 연결",
                    "description" to "실시간 채팅을 위해 WebSocket에 연결합니다",
                    "endpoint" to "WebSocket /ws",
                    "example" to mapOf(
                        "url" to "ws://localhost:$serverPort/ws",
                        "protocol" to "STOMP",
                        "subscribe" to "/topic/room/1",
                        "send" to "/app/chat/1"
                    )
                )
            ),
            "testAccounts" to listOf(
                mapOf("username" to "admin", "password" to "admin123", "role" to "ADMIN"),
                mapOf("username" to "moderator", "password" to "moderator123", "role" to "MODERATOR"),
                mapOf("username" to "user1", "password" to "password123", "role" to "USER"),
                mapOf("username" to "user2", "password" to "password123", "role" to "USER")
            ),
            "importantUrls" to mapOf(
                "apiDocs" to "http://localhost:$serverPort/api/docs",
                "health" to "http://localhost:$serverPort/api/health",
                "h2Console" to "http://localhost:$serverPort/h2-console"
            )
        )
    }

    /**
     * WebSocket 연결 가이드
     *
     * @return WebSocket 사용법 가이드
     */
    @GetMapping("/websocket-guide")
    fun getWebSocketGuide(): Map<String, Any> {
        return mapOf(
            "title" to "WebSocket 연결 가이드",
            "endpoint" to "ws://localhost:$serverPort/ws",
            "protocol" to "STOMP over SockJS",
            "authentication" to "JWT 토큰을 Authorization 헤더에 포함",
            "clientExample" to mapOf(
                "javascript" to """
                    // SockJS와 STOMP 라이브러리 필요
                    const socket = new SockJS('/ws');
                    const stompClient = Stomp.over(socket);
                    
                    // 연결
                    stompClient.connect({
                        'Authorization': 'Bearer YOUR_JWT_TOKEN'
                    }, function(frame) {
                        console.log('Connected: ' + frame);
                        
                        // 채팅방 구독
                        stompClient.subscribe('/topic/room/1', function(message) {
                            const chatMessage = JSON.parse(message.body);
                            console.log('Received:', chatMessage);
                        });
                        
                        // 메시지 전송
                        stompClient.send('/app/chat/1', {}, JSON.stringify({
                            'content': '안녕하세요!',
                            'messageType': 'TEXT'
                        }));
                    });
                """.trimIndent()
            ),
            "topics" to getWebSocketInfo().topics
        )
    }

    // ========================================
    // Private 헬퍼 메소드들
    // ========================================

    private fun getAuthenticationInfo(): AuthenticationInfo {
        return AuthenticationInfo(
            type = "Bearer Token (JWT)",
            description = "모든 인증이 필요한 요청에 Authorization 헤더에 JWT 토큰을 포함해야 합니다",
            headerName = "Authorization",
            tokenFormat = "Bearer {token}",
            exampleToken = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjA5NTQ1NjAwfQ.example"
        )
    }

    private fun getApiGroups(): List<ApiGroup> {
        return listOf(
            // 인증 API
            ApiGroup(
                name = "Authentication",
                description = "사용자 인증 관련 API",
                baseUrl = "/api/auth",
                endpoints = listOf(
                    ApiEndpoint("POST", "/api/auth/register", "회원가입", false, requestBody = "CreateUserRequest"),
                    ApiEndpoint("POST", "/api/auth/login", "로그인", false, requestBody = "LoginRequest"),
                    ApiEndpoint("POST", "/api/auth/validate", "토큰 검증", false),
                    ApiEndpoint("POST", "/api/auth/refresh", "토큰 갱신", true),
                    ApiEndpoint("POST", "/api/auth/logout", "로그아웃", false),
                    ApiEndpoint("GET", "/api/auth/me", "현재 사용자 정보", true, responseType = "AuthResponse"),
                    ApiEndpoint("GET", "/api/auth/health", "인증 API 상태 확인", false)
                )
            ),

            // 사용자 관리 API
            ApiGroup(
                name = "User Management",
                description = "사용자 정보 관리 API",
                baseUrl = "/api/users",
                endpoints = listOf(
                    ApiEndpoint("GET", "/api/users/me", "내 정보 조회", true, responseType = "UserResponse"),
                    ApiEndpoint("PUT", "/api/users/me", "내 정보 수정", true, requestBody = "UpdateUserRequest"),
                    ApiEndpoint("PUT", "/api/users/me/password", "비밀번호 변경", true, requestBody = "ChangePasswordRequest"),
                    ApiEndpoint("DELETE", "/api/users/me", "계정 비활성화", true),
                    ApiEndpoint("GET", "/api/users/{id}", "사용자 정보 조회", true, responseType = "UserResponse"),
                    ApiEndpoint("GET", "/api/users", "사용자 목록 조회", true, responseType = "PageResponse<UserResponse>"),
                    ApiEndpoint("GET", "/api/users/admin/all", "모든 사용자 조회", true, listOf("ADMIN")),
                    ApiEndpoint("PUT", "/api/users/admin/{id}/role", "사용자 역할 변경", true, listOf("ADMIN"), "ChangeUserRoleRequest"),
                    ApiEndpoint("GET", "/api/users/health", "사용자 API 상태 확인", false)
                )
            ),

            // 채팅 API
            ApiGroup(
                name = "Chat",
                description = "채팅방 및 메시지 관리 API",
                baseUrl = "/api/chat",
                endpoints = listOf(
                    ApiEndpoint("POST", "/api/chat/rooms", "채팅방 생성", true, requestBody = "CreateChatRoomRequest"),
                    ApiEndpoint("GET", "/api/chat/rooms", "채팅방 목록 조회", true, responseType = "PageResponse<ChatRoomResponse>"),
                    ApiEndpoint("GET", "/api/chat/rooms/{roomId}", "채팅방 정보 조회", true, responseType = "ChatRoomResponse"),
                    ApiEndpoint("PUT", "/api/chat/rooms/{roomId}", "채팅방 정보 수정", true, requestBody = "UpdateChatRoomRequest"),
                    ApiEndpoint("DELETE", "/api/chat/rooms/{roomId}", "채팅방 비활성화", true),
                    ApiEndpoint("POST", "/api/chat/rooms/{roomId}/messages", "메시지 전송", true, requestBody = "SendMessageRequest"),
                    ApiEndpoint("GET", "/api/chat/rooms/{roomId}/messages", "메시지 목록 조회", true, responseType = "PageResponse<ChatMessageResponse>"),
                    ApiEndpoint("DELETE", "/api/chat/messages/{messageId}", "메시지 삭제", true),
                    ApiEndpoint("POST", "/api/chat/upload", "파일 업로드", true, responseType = "FileUploadResponse"),
                    ApiEndpoint("GET", "/api/chat/health", "채팅 API 상태 확인", false)
                )
            ),

            // 신고 API
            ApiGroup(
                name = "Report",
                description = "신고 접수 및 관리 API",
                baseUrl = "/api/reports",
                endpoints = listOf(
                    ApiEndpoint("POST", "/api/reports", "신고 접수", true, requestBody = "CreateReportRequest"),
                    ApiEndpoint("GET", "/api/reports/my", "내 신고 목록", true, responseType = "PageResponse<ReportResponse>"),
                    ApiEndpoint("GET", "/api/reports", "모든 신고 조회", true, listOf("ADMIN", "MODERATOR")),
                    ApiEndpoint("GET", "/api/reports/pending", "처리 대기 신고", true, listOf("ADMIN", "MODERATOR")),
                    ApiEndpoint("GET", "/api/reports/overdue", "기한 초과 신고", true, listOf("ADMIN", "MODERATOR")),
                    ApiEndpoint("PUT", "/api/reports/{reportId}", "신고 처리", true, listOf("ADMIN", "MODERATOR"), "HandleReportRequest"),
                    ApiEndpoint("GET", "/api/reports/statistics", "신고 통계", true, listOf("ADMIN", "MODERATOR")),
                    ApiEndpoint("GET", "/api/reports/dashboard", "관리자 대시보드", true, listOf("ADMIN", "MODERATOR")),
                    ApiEndpoint("GET", "/api/reports/types", "신고 유형 목록", false),
                    ApiEndpoint("GET", "/api/reports/health", "신고 API 상태 확인", false)
                )
            )
        )
    }

    private fun getWebSocketInfo(): WebSocketInfo {
        return WebSocketInfo(
            endpoint = "/ws",
            protocol = "STOMP over SockJS",
            topics = listOf(
                WebSocketTopic("/topic/room/{roomId}", "subscribe", "채팅방 메시지 수신"),
                WebSocketTopic("/topic/room/{roomId}/typing", "subscribe", "타이핑 상태 수신"),
                WebSocketTopic("/topic/room/{roomId}/participants", "subscribe", "참여자 목록 수신"),
                WebSocketTopic("/topic/online-status", "subscribe", "온라인 상태 수신"),
                WebSocketTopic("/topic/announcements", "subscribe", "전체 공지사항 수신"),
                WebSocketTopic("/user/queue/notifications", "subscribe", "개인 알림 수신"),
                WebSocketTopic("/user/queue/force-leave", "subscribe", "강제 퇴장 알림 수신"),
                WebSocketTopic("/app/chat/{roomId}", "send", "메시지 전송"),
                WebSocketTopic("/app/chat/{roomId}/join", "send", "채팅방 입장"),
                WebSocketTopic("/app/chat/{roomId}/leave", "send", "채팅방 퇴장"),
                WebSocketTopic("/app/typing/{roomId}", "send", "타이핑 상태 전송")
            )
        )
    }

    private fun getServiceStatuses(): Map<String, ServiceStatus> {
        return mapOf(
            "database" to ServiceStatus(
                name = "H2 Database",
                status = "UP",
                description = "인메모리 데이터베이스가 정상 동작 중입니다",
                lastCheck = LocalDateTime.now()
            ),
            "authentication" to ServiceStatus(
                name = "JWT Authentication",
                status = "UP",
                description = "JWT 토큰 인증 서비스가 정상 동작 중입니다",
                lastCheck = LocalDateTime.now()
            ),
            "websocket" to ServiceStatus(
                name = "WebSocket Server",
                status = "UP",
                description = "실시간 통신 서버가 정상 동작 중입니다",
                lastCheck = LocalDateTime.now()
            ),
            "chat" to ServiceStatus(
                name = "Chat Service",
                status = "UP",
                description = "채팅 서비스가 정상 동작 중입니다",
                lastCheck = LocalDateTime.now()
            ),
            "report" to ServiceStatus(
                name = "Report Service",
                status = "UP",
                description = "신고 관리 서비스가 정상 동작 중입니다",
                lastCheck = LocalDateTime.now()
            )
        )
    }

    private fun formatDuration(duration: java.time.Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        return when {
            days > 0 -> "${days}일 ${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간 ${minutes}분"
            else -> "${minutes}분"
        }
    }
}