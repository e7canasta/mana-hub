package com.hub.clients.identity

import com.fasterxml.jackson.annotation.JsonProperty

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val userId: String,
    val username: String,
    val role: Role,
    val token: String? = null
)

data class CreateUserRequest(
    val username: String,
    @JsonProperty("displayName") val displayName: String,
    val role: Role,
    val password: String,
    val jobTitle: String? = null
)

data class UpdateUserRequest(
    val displayName: String? = null,
    val jobTitle: String? = null
)

data class UserResponse(
    val id: String,
    val username: String,
    @JsonProperty("displayName") val displayName: String,
    val role: Role,
    val jobTitle: String? = null,
    @JsonProperty("isRetired") val isRetired: Boolean = false
)

enum class Role { OWNER, SUPERVISOR, STAFF }
