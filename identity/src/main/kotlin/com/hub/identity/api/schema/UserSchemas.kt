package com.hub.identity.api.schema

import com.hub.identity.domain.model.Role
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateUserRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val displayName: String,
    @field:NotNull val role: Role,
    val jobTitle: String? = null,
    @field:NotBlank val password: String
)

data class UpdateUserRequest(
    val displayName: String? = null,
    val jobTitle: String? = null
)

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)
