package com.hub.clients.streams

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateStreamRequest(
    val streamKey: String,
    val name: String? = null
)

data class StreamResponse(
    val id: String,
    @JsonProperty("roomId") val roomId: String,
    @JsonProperty("streamKey") val streamKey: String,
    val name: String? = null,
    val active: Boolean = true
)

data class StreamRegionResponse(
    val id: String,
    @JsonProperty("streamId") val streamId: String,
    @JsonProperty("regionType") val regionType: RegionType,
    val points: String,
    val label: String? = null,
    @JsonProperty("isStatic") val isStatic: Boolean = true
)

data class ReplaceRegionsRequest(
    val regions: List<CreateRegionRequest>
)

data class CreateRegionRequest(
    @JsonProperty("regionType") val regionType: RegionType,
    val points: String,
    val label: String? = null
)

data class UpdateRegionRequest(
    val points: String? = null,
    val label: String? = null
)

enum class RegionType { BATHROOM, HALLWAY, EXIT, BED, FURNITURE, PERSON, OBJECT }
