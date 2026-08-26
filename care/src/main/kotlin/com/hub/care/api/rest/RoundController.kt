package com.hub.care.api.rest

import com.hub.care.application.dto.*
import com.hub.care.application.service.RoundApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class RoundController(
    private val roundApplicationService: RoundApplicationService
) {

    @PostMapping("/rounds")
    fun createRound(@Valid @RequestBody request: CreateRoundRequest): ResponseEntity<RoundResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(roundApplicationService.createRound(request))
    }

    @GetMapping("/rounds/current")
    fun getCurrentRound(@RequestParam wingId: String): ResponseEntity<RoundResponse> {
        val round = roundApplicationService.getCurrentRound(wingId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(round)
    }

    @GetMapping("/rounds")
    fun listRounds(@RequestParam wingId: String): ResponseEntity<List<RoundResponse>> {
        return ResponseEntity.ok(roundApplicationService.listRounds(wingId))
    }

    @GetMapping("/rounds/{roundId}")
    fun getRound(@PathVariable roundId: String): ResponseEntity<RoundResponse> {
        return ResponseEntity.ok(roundApplicationService.getRound(roundId))
    }

    @PatchMapping("/rounds/{roundId}")
    fun updateRound(
        @PathVariable roundId: String,
        @RequestParam(required = false) actorId: String?
    ): ResponseEntity<RoundResponse> {
        return ResponseEntity.ok(roundApplicationService.updateRound(roundId, actorId ?: "system"))
    }

    @PatchMapping("/round-tasks/{taskId}")
    fun updateRoundTask(
        @PathVariable taskId: String,
        @Valid @RequestBody request: UpdateRoundTaskRequest
    ): ResponseEntity<RoundTaskResponse> {
        return ResponseEntity.ok(roundApplicationService.updateRoundTask(taskId, request))
    }
}
