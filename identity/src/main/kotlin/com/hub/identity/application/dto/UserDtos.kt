package com.hub.identity.application.dto

import com.hub.identity.domain.model.Role
import java.time.Instant

data class CreateUserRequest(
    val username: String,
    val displayName: String,
    val role: Role,
    val jobTitle: String?,
    val password: String
)

data class UpdateUserRequest(
    val displayName: String?,
    val jobTitle: String?
)

data class UserResponse(
    val id: String,
    val username: String,
    val displayName: String,
    val role: Role,
    val jobTitle: String?,
    val isRetired: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserResponse,
    val expiresAt: Instant
)
