package com.example.kopringCRUD.global.config


import com.example.kopringCRUD.domain.chat.entity.ChatRoom
import com.example.kopringCRUD.domain.chat.entity.ChatRoomInfo
import com.example.kopringCRUD.domain.chat.repository.ChatRoomRepository
import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.domain.user.entity.UserCredentials
import com.example.kopringCRUD.domain.user.entity.UserProfile
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.domain.user.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@Profile("!test") // 테스트 환경에서는 실행되지 않음
class DataInitializer {

    @Bean
    fun initData(
        userRepository: UserRepository,
        chatRoomRepository: ChatRoomRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner {
        return CommandLineRunner { args ->

            // 🔐 관리자 계정 생성
            if (!userRepository.existsByUsername("admin")) {
                val admin = User(
                    profile = UserProfile(
                        username = "admin",
                        email = "admin@example.com",
                        displayName = "시스템 관리자"
                    ),
                    credentials = UserCredentials(
                        password = passwordEncoder.encode("admin123")
                    ),
                    role = UserRole.ADMIN
                )
                userRepository.save(admin)
                println("✅ 관리자 계정 생성완료: admin/admin123")
            }

            // 👤 테스트 사용자 1 생성
            if (!userRepository.existsByUsername("user1")) {
                val user1 = User(
                    profile = UserProfile(
                        username = "user1",
                        email = "user1@example.com",
                        displayName = "사용자 1"
                    ),
                    credentials = UserCredentials(
                        password = passwordEncoder.encode("password123")
                    )
                )
                val savedUser1 = userRepository.save(user1)
                println("✅ 테스트 사용자 생성완료: user1/password123")

                // 💬 샘플 채팅방 생성
                val sampleRoom = ChatRoom(
                    roomInfo = ChatRoomInfo(
                        name = "일반 채팅방",
                        description = "누구나 참여할 수 있는 일반 채팅방입니다.",
                        maxParticipants = 50
                    ),
                    creator = savedUser1
                )
                chatRoomRepository.save(sampleRoom)
                println("✅ 샘플 채팅방 생성완료")
            }

            // 👤 테스트 사용자 2 생성
            if (!userRepository.existsByUsername("user2")) {
                val user2 = User(
                    profile = UserProfile(
                        username = "user2",
                        email = "user2@example.com",
                        displayName = "사용자 2"
                    ),
                    credentials = UserCredentials(
                        password = passwordEncoder.encode("password123")
                    )
                )
                userRepository.save(user2)
                println("✅ 테스트 사용자 생성완료: user2/password123")
            }

            // 🛡️ 모더레이터 계정 생성
            if (!userRepository.existsByUsername("moderator")) {
                val moderator = User(
                    profile = UserProfile(
                        username = "moderator",
                        email = "moderator@example.com",
                        displayName = "채팅 관리자"
                    ),
                    credentials = UserCredentials(
                        password = passwordEncoder.encode("moderator123")
                    ),
                    role = UserRole.MODERATOR
                )
                userRepository.save(moderator)
                println("✅ 모더레이터 계정 생성완료: moderator/moderator123")
            }

            println("🚀 애플리케이션 시작 완료!")
            println("📊 H2 콘솔: http://localhost:8080/h2-console")
            println("🔐 JDBC URL: jdbc:h2:mem:chatdb")
            println("📝 API 문서: http://localhost:8080/api/docs")
            println("💊 헬스체크: http://localhost:8080/api/health")
            println("")
            println("👥 기본 계정 정보:")
            println("   관리자: admin/admin123")
            println("   모더레이터: moderator/moderator123")
            println("   사용자1: user1/password123")
            println("   사용자2: user2/password123")
        }
    }
}