package com.example.kopringCRUD.global.config

import com.example.kopringCRUD.domain.chat.entity.ChatRoom
import com.example.kopringCRUD.domain.chat.entity.ChatRoomInfo
import com.example.kopringCRUD.domain.chat.repository.ChatRoomRepository
import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.domain.user.entity.UserRole
import com.example.kopringCRUD.domain.user.entity.UserProfile
import com.example.kopringCRUD.domain.user.entity.UserCredentials
import com.example.kopringCRUD.domain.user.repository.UserRepository
import com.example.kopringCRUD.global.common.PageRequest
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 애플리케이션 시작 시 기본 데이터를 초기화하는 컴포넌트
 * MySQL 데이터베이스에 필요한 초기 데이터를 생성합니다.
 */
@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        initializeUsers()
        initializeChatRooms()
        printStartupInfo()
    }

    /**
     * 기본 사용자 계정들을 생성합니다.
     */
    private fun initializeUsers() {
        // 관리자 계정 생성
        if (!userRepository.existsByUsername("admin")) {
            val admin = User(
                profile = UserProfile(
                    username = "admin",
                    email = "admin@example.com",
                    displayName = "관리자"
                ),
                credentials = UserCredentials(
                    password = passwordEncoder.encode("admin123")
                ),
                role = UserRole.ADMIN
            )
            userRepository.save(admin)
            println("✅ 관리자 계정 생성완료: admin/admin123")
        }

        // 테스트 사용자1 생성
        if (!userRepository.existsByUsername("user1")) {
            val user1 = User(
                profile = UserProfile(
                    username = "user1",
                    email = "user1@example.com",
                    displayName = "사용자1"
                ),
                credentials = UserCredentials(
                    password = passwordEncoder.encode("password123")
                ),
                role = UserRole.USER
            )
            userRepository.save(user1)
            println("✅ 테스트 사용자 생성완료: user1/password123")
        }

        // 테스트 사용자2 생성
        if (!userRepository.existsByUsername("user2")) {
            val user2 = User(
                profile = UserProfile(
                    username = "user2",
                    email = "user2@example.com",
                    displayName = "사용자2"
                ),
                credentials = UserCredentials(
                    password = passwordEncoder.encode("password123")
                ),
                role = UserRole.USER
            )
            userRepository.save(user2)
            println("✅ 테스트 사용자 생성완료: user2/password123")
        }

        // 모더레이터 계정 생성
        if (!userRepository.existsByUsername("moderator")) {
            val moderator = User(
                profile = UserProfile(
                    username = "moderator",
                    email = "moderator@example.com",
                    displayName = "모더레이터"
                ),
                credentials = UserCredentials(
                    password = passwordEncoder.encode("moderator123")
                ),
                role = UserRole.MODERATOR
            )
            userRepository.save(moderator)
            println("✅ 모더레이터 계정 생성완료: moderator/moderator123")
        }
    }

    /**
     * 기본 채팅방을 생성합니다.
     */
    private fun initializeChatRooms() {
        // 활성 채팅방이 없을 때만 기본 채팅방 생성
        if (chatRoomRepository.countActiveRooms() == 0L) {
            val creatorOptional = userRepository.findByUsername("user1")
            if (creatorOptional.isPresent) {
                val creator = creatorOptional.get()
                val roomInfo = ChatRoomInfo(
                    name = "일반 채팅방",
                    description = "자유롭게 대화할 수 있는 공간입니다.",
                    maxParticipants = 50
                )

                val sampleRoom = ChatRoom(
                    roomInfo = roomInfo,
                    creator = creator,
                    isActive = true
                )

                chatRoomRepository.save(sampleRoom)
                println("✅ 샘플 채팅방 생성완료")
            } else {
                println("⚠️ user1을 찾을 수 없어 채팅방 생성을 건너뜁니다.")
            }
        } else {
            println("ℹ️ 이미 활성 채팅방이 존재합니다.")
        }
    }

    /**
     * 시작 정보를 출력합니다.
     */
    private fun printStartupInfo() {
        println("🚀 애플리케이션 시작 완료!")
        println("🗄️ 데이터베이스: MySQL")
        println("📝 API 문서: http://localhost:8080/api/docs")
        println("💊 헬스체크: http://localhost:8080/api/health")
        println()
        println("👥 기본 계정 정보:")
        println("   관리자: admin/admin123")
        println("   모더레이터: moderator/moderator123")
        println("   사용자1: user1/password123")
        println("   사용자2: user2/password123")
        println()
        println("🏠 채팅방 정보:")
        val activeRoomCount = chatRoomRepository.countActiveRooms()
        println("   활성 채팅방 수: ${activeRoomCount}개")
    }
}