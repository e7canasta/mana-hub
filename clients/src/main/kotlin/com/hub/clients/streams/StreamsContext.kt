package com.hub.clients.streams

import com.hub.clients.core.HttpApi
import com.hub.clients.core.StreamsDsl

@StreamsDsl
class StreamsScope internal constructor(private val http: HttpApi) {

    fun assignStreamToRoom(roomId: String, block: StreamBuilder.() -> Unit = {}): Stream {
        val builder = StreamBuilder().apply(block)
        val resp = http.post(
            "/api/v1/rooms/$roomId/streams",
            CreateStreamRequest(builder.streamKey, builder.name),
            StreamResponse::class.java
        )
        return Stream(http, resp)
    }

    fun roomStreams(roomId: String): List<StreamResponse> =
        http.get("/api/v1/rooms/$roomId/streams", Array<StreamResponse>::class.java).toList()

    fun streamById(id: String): StreamResponse =
        http.get("/api/v1/streams/$id", StreamResponse::class.java)
}

@StreamsDsl
class StreamBuilder {
    var streamKey: String = ""
    var name: String? = null
}

class Stream internal constructor(
    private val http: HttpApi,
    val raw: StreamResponse
) {
    val id: String get() = raw.id
    val streamKey: String get() = raw.streamKey
    val name: String? get() = raw.name
    val active: Boolean get() = raw.active

    fun defineRegions(block: RegionSetBuilder.() -> Unit): List<StreamRegionResponse> {
        val builder = RegionSetBuilder().apply(block)
        val request = ReplaceRegionsRequest(builder.regions.map {
            CreateRegionRequest(it.regionType, it.points, it.label)
        })
        return http.put("/api/v1/streams/$id/regions", request, Array<StreamRegionResponse>::class.java).toList()
    }

    fun regions(): List<StreamRegionResponse> =
        http.get("/api/v1/streams/$id/regions", Array<StreamRegionResponse>::class.java).toList()

    override fun toString(): String = "Stream($streamKey, active=$active)"
}

@StreamsDsl
class RegionSetBuilder {
    internal val regions = mutableListOf<RegionDef>()

    fun region(regionType: RegionType, points: String, label: String? = null) {
        regions.add(RegionDef(regionType, points, label))
    }

    fun bed(points: String, label: String? = "Bed zone") = region(RegionType.BED, points, label)
    fun hallway(points: String, label: String? = "Hallway") = region(RegionType.HALLWAY, points, label)
    fun exit(points: String, label: String? = "Exit") = region(RegionType.EXIT, points, label)
    fun bathroom(points: String, label: String? = "Bathroom") = region(RegionType.BATHROOM, points, label)
}

data class RegionDef(
    val regionType: RegionType,
    val points: String,
    val label: String?
)
