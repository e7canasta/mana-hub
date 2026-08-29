package com.hub.identity.domain.service

import com.hub.identity.domain.model.User
import com.hub.shared.domain.UserId
import com.hub.identity.domain.repository.UserRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class AuthenticationService(
    private val userRepository: UserRepository
) {
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }

    fun authenticate(username: String, password: String): User? {
        val user = userRepository.findByUsername(username) ?: return null
        if (user.isRetired) return null
        if (!verifyPassword(password, user.passwordHash)) return null
        return user
    }
}
