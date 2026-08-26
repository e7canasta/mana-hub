package com.hub.streams.application.dto

data class CreateStreamRequest(
    val streamKey: String,
    val name: String? = null
)

data class StreamResponse(
    val id: String,
    val roomId: String,
    val streamKey: String,
    val name: String?,
    val active: Boolean
)

data class StreamRegionResponse(
    val id: String,
    val streamId: String,
    val regionType: com.hub.streams.domain.model.RegionType,
    val points: String,
    val label: String?,
    val isStatic: Boolean
)

data class ReplaceRegionsRequest(
    val regions: List<CreateRegionRequest>
)

data class CreateRegionRequest(
    val regionType: com.hub.streams.domain.model.RegionType,
    val points: String,
    val label: String? = null
)

data class UpdateRegionRequest(
    val points: String? = null,
    val label: String? = null
)
