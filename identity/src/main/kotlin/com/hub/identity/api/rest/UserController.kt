package com.hub.identity.api.rest

import com.hub.identity.api.schema.CreateUserRequest
import com.hub.identity.api.schema.UpdateUserRequest
import com.hub.identity.application.dto.CreateUserRequest as AppCreateUserRequest
import com.hub.identity.application.dto.UpdateUserRequest as AppUpdateUserRequest
import com.hub.identity.application.dto.UserResponse
import com.hub.identity.application.service.UserApplicationService
import com.hub.shared.domain.UserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userApplicationService: UserApplicationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@Valid @RequestBody request: CreateUserRequest): UserResponse {
        val appRequest = AppCreateUserRequest(
            username = request.username,
            displayName = request.displayName,
            role = request.role,
            jobTitle = request.jobTitle,
            password = request.password
        )
        return userApplicationService.createUser(appRequest)
    }

    @GetMapping
    fun listUsers(): List<UserResponse> =
        userApplicationService.listUsers()

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: String): UserResponse =
        userApplicationService.getUserById(UserId(userId))

    @PatchMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: String,
        @Valid @RequestBody request: UpdateUserRequest
    ): UserResponse {
        val appRequest = AppUpdateUserRequest(
            displayName = request.displayName,
            jobTitle = request.jobTitle
        )
        return userApplicationService.updateUser(UserId(userId), appRequest)
    }
}
