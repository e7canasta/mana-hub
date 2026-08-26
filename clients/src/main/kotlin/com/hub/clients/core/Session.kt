package com.hub.clients.core

import com.hub.clients.identity.Role

data class Session internal constructor(
    val userId: String,
    val username: String,
    val role: Role,
    val token: String? = null
)
