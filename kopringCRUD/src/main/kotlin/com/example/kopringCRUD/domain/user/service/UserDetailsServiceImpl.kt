package com.example.kopringCRUD.domain.user.service


import com.example.kopringCRUD.domain.user.entity.User
import com.example.kopringCRUD.domain.user.repository.UserRepository
import com.example.kopringCRUD.global.security.UserPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Spring Security UserDetailsService 구현체
 * 사용자 인증 시 사용자 정보를 로드하는 역할
 */
@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    /**
     * 사용자명으로 사용자 정보를 로드
     * Spring Security에서 인증 시 자동으로 호출됨
     */
    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("사용자를 찾을 수 없습니다: $username") }

        return user.toPrincipal()
    }

    /**
     * ID로 사용자 정보를 로드 (JWT 필터에서 사용 가능)
     */
    @Transactional(readOnly = true)
    fun loadUserById(id: Long): UserDetails {
        val user = userRepository.findById(id)
            .orElseThrow { UsernameNotFoundException("사용자를 찾을 수 없습니다: $id") }

        return user.toPrincipal()
    }
}

/**
 * User 엔티티를 UserPrincipal로 변환
 * Spring Security에서 사용할 수 있는 형태로 변환
 */
fun User.toPrincipal(): UserPrincipal {
    // 사용자 권한 생성 (ROLE_ 접두사 추가)
    val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    return UserPrincipal(
        id = id,
        username = profile.username,
        email = profile.email,
        password = credentials.password,
        authorities = authorities,
        isActive = isActive
    )
}