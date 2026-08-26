package com.hub.identity.application.service

import com.hub.identity.application.dto.CreateUserRequest
import com.hub.identity.application.dto.UpdateUserRequest
import com.hub.identity.application.dto.UserResponse
import com.hub.identity.domain.model.User
import com.hub.identity.domain.model.UserId
import com.hub.identity.domain.repository.UserRepository
import com.hub.identity.domain.service.AuthenticationService
import com.hub.shared.domain.DomainEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserApplicationService(
    private val userRepository: UserRepository,
    private val authenticationService: AuthenticationService,
    private val eventPublisher: DomainEventPublisher
) {

    @Transactional
    fun createUser(request: CreateUserRequest): UserResponse {
        require(!userRepository.existsByUsername(request.username)) {
            "Username '${request.username}' already exists"
        }

        val passwordHash = authenticationService.hashPassword(request.password)
        val user = User.create(
            username = request.username,
            displayName = request.displayName,
            role = request.role,
            jobTitle = request.jobTitle,
            passwordHash = passwordHash
        )

        val saved = userRepository.save(user)
        eventPublisher.publishAll(saved.pullEvents())
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun getUserById(id: UserId): UserResponse {
        val user = userRepository.findById(id)
            ?: throw IllegalArgumentException("User not found: $id")
        return user.toResponse()
    }

    @Transactional(readOnly = true)
    fun listUsers(): List<UserResponse> {
        return userRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun updateUser(id: UserId, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(id)
            ?: throw IllegalArgumentException("User not found: $id")

        val updated = user.updateProfile(
            displayName = request.displayName,
            jobTitle = request.jobTitle
        )

        val saved = userRepository.save(updated)
        return saved.toResponse()
    }

    @Transactional
    fun retireUser(id: UserId, actorId: UserId): UserResponse {
        val user = userRepository.findById(id)
            ?: throw IllegalArgumentException("User not found: $id")

        val retired = user.retire(actorId)
        val saved = userRepository.save(retired)
        eventPublisher.publishAll(saved.pullEvents())
        return saved.toResponse()
    }

    private fun User.toResponse(): UserResponse = UserResponse(
        id = id.value,
        username = username,
        displayName = displayName,
        role = role,
        jobTitle = jobTitle,
        isRetired = isRetired,
        createdAt = null,
        updatedAt = null
    )
}
