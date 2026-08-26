package com.hub.clients.identity

import com.hub.clients.core.HttpApi
import com.hub.clients.core.IdentityDsl
import com.hub.clients.core.Session

@IdentityDsl
class IdentityScope internal constructor(private val http: HttpApi) {

    fun login(username: String, password: String): Session {
        val req = LoginRequest(username, password)
        val resp = http.post("/api/v1/auth/login", req, LoginResponse::class.java)
        return Session(resp.userId, resp.username, resp.role, resp.token)
    }

    fun registerUser(block: UserBuilder.() -> Unit): User {
        val builder = UserBuilder().apply(block)
        val resp = http.post("/api/v1/users", builder.toRequest(), UserResponse::class.java)
        return User(http, resp)
    }

    fun registerOwner(
        username: String = "owner",
        displayName: String = "Owner",
        password: String = "secret",
        block: UserBuilder.() -> Unit = {}
    ): User = registerUser {
        this.username = username
        this.displayName = displayName
        this.role = Role.OWNER
        this.password = password
        apply(block)
    }

    fun registerSupervisor(
        username: String = "supervisor",
        displayName: String = "Supervisor",
        password: String = "secret",
        block: UserBuilder.() -> Unit = {}
    ): User = registerUser {
        this.username = username
        this.displayName = displayName
        this.role = Role.SUPERVISOR
        this.password = password
        apply(block)
    }

    fun registerStaff(
        username: String = "staff",
        displayName: String = "Staff",
        password: String = "secret",
        block: UserBuilder.() -> Unit = {}
    ): User = registerUser {
        this.username = username
        this.displayName = displayName
        this.role = Role.STAFF
        this.password = password
        apply(block)
    }
}

@IdentityDsl
class UserBuilder {
    var username: String = ""
    var displayName: String = ""
    var role: Role = Role.OWNER
    var password: String = ""
    var jobTitle: String? = null

    internal fun toRequest() = CreateUserRequest(username, displayName, role, password, jobTitle)
}

class User internal constructor(
    private val http: HttpApi,
    val raw: UserResponse
) {
    val id: String get() = raw.id
    val username: String get() = raw.username
    val displayName: String get() = raw.displayName
    val role: Role get() = raw.role
    val jobTitle: String? get() = raw.jobTitle
    val isRetired: Boolean get() = raw.isRetired

    fun update(displayName: String? = null, jobTitle: String? = null): UserResponse =
        http.patch("/api/v1/users/$id", UpdateUserRequest(displayName, jobTitle), UserResponse::class.java)

    override fun toString(): String = "User($username, $role)"
}
