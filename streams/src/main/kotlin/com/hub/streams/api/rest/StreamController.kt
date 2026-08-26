package com.hub.streams.api.rest

import com.hub.streams.application.dto.*
import com.hub.streams.application.service.StreamApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class StreamController(
    private val streamApplicationService: StreamApplicationService
) {

    @PostMapping("/rooms/{roomId}/streams")
    fun createStream(
        @PathVariable roomId: String,
        @Valid @RequestBody request: CreateStreamRequest
    ): ResponseEntity<StreamResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(streamApplicationService.createStream(roomId, request))
    }

    @GetMapping("/rooms/{roomId}/streams")
    fun listStreams(@PathVariable roomId: String): ResponseEntity<List<StreamResponse>> {
        return ResponseEntity.ok(streamApplicationService.listStreams(roomId))
    }

    @GetMapping("/streams/{streamId}")
    fun getStream(@PathVariable streamId: String): ResponseEntity<StreamResponse> {
        return ResponseEntity.ok(streamApplicationService.getStream(streamId))
    }

    @GetMapping("/streams/{streamId}/regions")
    fun listRegions(@PathVariable streamId: String): ResponseEntity<List<StreamRegionResponse>> {
        return ResponseEntity.ok(streamApplicationService.listRegions(streamId))
    }

    @PutMapping("/streams/{streamId}/regions")
    fun replaceRegions(
        @PathVariable streamId: String,
        @Valid @RequestBody request: ReplaceRegionsRequest
    ): ResponseEntity<List<StreamRegionResponse>> {
        return ResponseEntity.ok(streamApplicationService.replaceRegions(streamId, request))
    }

    @PatchMapping("/streams/{streamId}/regions/{regionId}")
    fun updateRegion(
        @PathVariable streamId: String,
        @PathVariable regionId: String,
        @Valid @RequestBody request: UpdateRegionRequest
    ): ResponseEntity<StreamRegionResponse> {
        return ResponseEntity.ok(streamApplicationService.updateRegion(streamId, regionId, request))
    }
}
