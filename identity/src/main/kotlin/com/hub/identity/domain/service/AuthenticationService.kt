package com.hub.identity.domain.service

import com.hub.identity.domain.model.User
import com.hub.shared.domain.UserId
import com.hub.identity.domain.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val userRepository: UserRepository
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun hashPassword(password: String): String = passwordEncoder.encode(password).toString()

    fun verifyPassword(password: String, hash: String): Boolean = passwordEncoder.matches(password, hash)

    fun authenticate(username: String, password: String): User? {
        val user = userRepository.findByUsername(username) ?: return null
        if (user.isRetired) return null
        if (!verifyPassword(password, user.passwordHash)) return null
        return user
    }
}
