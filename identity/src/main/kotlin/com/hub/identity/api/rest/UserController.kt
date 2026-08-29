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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userApplicationService: UserApplicationService
) {

    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val appRequest = AppCreateUserRequest(
            username = request.username,
            displayName = request.displayName,
            role = request.role,
            jobTitle = request.jobTitle,
            password = request.password
        )
        val response = userApplicationService.createUser(appRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun listUsers(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(userApplicationService.listUsers())
    }

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: String): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userApplicationService.getUserById(UserId(userId)))
    }

    @PatchMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: String,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        val appRequest = AppUpdateUserRequest(
            displayName = request.displayName,
            jobTitle = request.jobTitle
        )
        return ResponseEntity.ok(userApplicationService.updateUser(UserId(userId), appRequest))
    }
}
