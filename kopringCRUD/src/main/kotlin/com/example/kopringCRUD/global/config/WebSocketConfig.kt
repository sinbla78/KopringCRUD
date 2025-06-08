package com.example.kopringCRUD.global.config

import com.example.kopringCRUD.global.security.JwtUtil
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketJwtInterceptor: WebSocketJwtInterceptor
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic", "/queue")
        config.setApplicationDestinationPrefixes("/app")
        config.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(webSocketJwtInterceptor)
    }
}

/**
 * WebSocket JWT 인증 인터셉터
 */
@Component
class WebSocketJwtInterceptor(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: UserDetailsService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor != null && StompCommand.CONNECT == accessor.command) {
            val authorization = accessor.getNativeHeader("Authorization")?.firstOrNull()

            if (authorization != null && authorization.startsWith("Bearer ")) {
                val token = authorization.substring(7)

                try {
                    if (jwtUtil.validateToken(token)) {
                        val username = jwtUtil.getUsernameFromToken(token)
                        val userDetails = userDetailsService.loadUserByUsername(username)

                        val authentication = UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.authorities
                        )

                        accessor.user = authentication
                        println("✅ WebSocket JWT 인증 성공: $username")
                    } else {
                        println("❌ WebSocket JWT 토큰 검증 실패")
                    }
                } catch (e: Exception) {
                    println("❌ WebSocket JWT 처리 중 에러: ${e.message}")
                }
            } else {
                println("⚠️ WebSocket Authorization 헤더가 없음")
            }
        }

        return message
    }
}