package com.hub.care.api.rest

import com.hub.care.application.dto.*
import com.hub.care.application.service.RoundApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class RoundController(
    private val roundApplicationService: RoundApplicationService
) {

    @PostMapping("/rounds")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRound(@Valid @RequestBody request: CreateRoundRequest): RoundResponse =
        roundApplicationService.createRound(request)

    @GetMapping("/rounds/current")
    fun getCurrentRound(@RequestParam wingId: String): RoundResponse? =
        roundApplicationService.getCurrentRound(wingId)

    @GetMapping("/rounds")
    fun listRounds(@RequestParam wingId: String): List<RoundResponse> =
        roundApplicationService.listRounds(wingId)

    @GetMapping("/rounds/{roundId}")
    fun getRound(@PathVariable roundId: String): RoundResponse =
        roundApplicationService.getRound(roundId)

    @PatchMapping("/rounds/{roundId}")
    fun updateRound(
        @PathVariable roundId: String,
        @RequestParam(required = false) actorId: String?
    ): RoundResponse =
        roundApplicationService.updateRound(roundId, actorId ?: "system")

    @PatchMapping("/round-tasks/{taskId}")
    fun updateRoundTask(
        @PathVariable taskId: String,
        @Valid @RequestBody request: UpdateRoundTaskRequest
    ): RoundTaskResponse =
        roundApplicationService.updateRoundTask(taskId, request)
}
