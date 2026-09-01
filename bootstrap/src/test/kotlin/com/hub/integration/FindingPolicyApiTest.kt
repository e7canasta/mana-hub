package com.hub.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType

class FindingPolicyApiTest : IntegrationTestBase() {

    @BeforeEach
    fun setUp() {
        cleanDatabase()
    }

    private val fullSleep = mapOf(
        "restlessHighEnabled" to true,
        "restlessHighThreshold" to 0.25,
        "restlessFragmentedEnabled" to true,
        "restlessFragmentedThreshold" to 0.35,
        "exitsRisingEnabled" to true,
        "exitsRisingFactor" to 1.15,
        "exitsRisingMinDelta" to 0.3,
        "sleepInRangeEnabled" to true,
        "sleepInRangeThreshold" to 0.20,
        "dropWoWEnabled" to true,
        "dropWoWMinutes" to 45,
        "dawnClusterEnabled" to true,
        "dawnFrom" to "05:00",
        "dawnTo" to "06:05",
        "dawnMinCount" to 3,
        "dawnRatio" to 0.66,
    )

    private val fullCare = mapOf(
        "careThinEnabled" to true,
        "careThinMinutes" to 20.0,
    )

    private val fullBathroom = mapOf(
        "bathroomNightEnabled" to true,
        "nightMinAvg" to 1.0,
        "nightRiseFactor" to 1.5,
    )

    // ─── Default Policy ───────────────────────────────────────────────

    @Test
    fun `given no default exists, when GET default, then auto-creates with all rules enabled`() {
        val policy = getMap("/api/v1/insights/policies/default")

        assertThat(policy["default"]).isEqualTo(true)
        assertThat(policy["residentId"]).isNull()

        @Suppress("UNCHECKED_CAST")
        val sleep = policy["sleep"] as Map<String, Any>
        assertThat(sleep["restlessHighEnabled"]).isEqualTo(true)
        assertThat(sleep["dawnClusterEnabled"]).isEqualTo(true)
        assertThat(sleep["exitsRisingEnabled"]).isEqualTo(true)
        assertThat(sleep["sleepInRangeEnabled"]).isEqualTo(true)
        assertThat(sleep["dropWoWEnabled"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val care = policy["care"] as Map<String, Any>
        assertThat(care["careThinEnabled"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val bathroom = policy["bathroom"] as Map<String, Any>
        assertThat(bathroom["bathroomNightEnabled"]).isEqualTo(true)
    }

    @Test
    fun `given default already exists, when GET default again, then returns same default`() {
        getMap("/api/v1/insights/policies/default")
        val second = getMap("/api/v1/insights/policies/default")

        assertThat(second["default"]).isEqualTo(true)
    }

    @Test
    fun `given default exists, when PUT default threshold, then threshold updates`() {
        val updated = putMap("/api/v1/insights/policies/default", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighThreshold", 0.35) })
        ))

        @Suppress("UNCHECKED_CAST")
        val sleep = updated["sleep"] as Map<String, Any>
        assertThat(sleep["restlessHighThreshold"]).isEqualTo(0.35)
        assertThat(sleep["restlessHighEnabled"]).isEqualTo(true)
    }

    @Test
    fun `given default exists, when PUT disable careThinEnabled, then flag is false`() {
        val updated = putMap("/api/v1/insights/policies/default", mapOf(
            "care" to (fullCare.toMutableMap().apply { put("careThinEnabled", false) })
        ))

        @Suppress("UNCHECKED_CAST")
        val care = updated["care"] as Map<String, Any>
        assertThat(care["careThinEnabled"]).isEqualTo(false)
    }

    // ─── Per-Resident Policy ──────────────────────────────────────────

    @Test
    fun `given jose has no policy, when GET jose, then cascades to default`() {
        val policy = getMap("/api/v1/insights/policies/jose")

        assertThat(policy["default"]).isEqualTo(true)
        assertThat(policy["residentId"]).isNull()
    }

    @Test
    fun `given jose needs custom policy, when PUT jose, then creates resident policy`() {
        val created = putMap("/api/v1/insights/policies/jose", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply {
                put("restlessHighEnabled", false)
                put("restlessHighThreshold", 0.30)
            })
        ))

        assertThat(created["residentId"]).isEqualTo("jose")
        assertThat(created["default"]).isEqualTo(false)

        @Suppress("UNCHECKED_CAST")
        val sleep = created["sleep"] as Map<String, Any>
        assertThat(sleep["restlessHighEnabled"]).isEqualTo(false)
        assertThat(sleep["restlessHighThreshold"]).isEqualTo(0.30)
    }

    @Test
    fun `given jose has custom policy, when GET jose, then returns his policy not default`() {
        putMap("/api/v1/insights/policies/jose", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighEnabled", false) })
        ))

        val policy = getMap("/api/v1/insights/policies/jose")

        assertThat(policy["residentId"]).isEqualTo("jose")
        assertThat(policy["default"]).isEqualTo(false)

        @Suppress("UNCHECKED_CAST")
        val sleep = policy["sleep"] as Map<String, Any>
        assertThat(sleep["restlessHighEnabled"]).isEqualTo(false)
    }

    @Test
    fun `given jose has custom policy, when PUT update jose, then merges without losing other flags`() {
        putMap("/api/v1/insights/policies/jose", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighEnabled", false) })
        ))

        val updated = putMap("/api/v1/insights/policies/jose", mapOf(
            "care" to (fullCare.toMutableMap().apply {
                put("careThinEnabled", false)
                put("careThinMinutes", 15.0)
            })
        ))

        assertThat(updated["residentId"]).isEqualTo("jose")

        @Suppress("UNCHECKED_CAST")
        val care = updated["care"] as Map<String, Any>
        assertThat(care["careThinEnabled"]).isEqualTo(false)
        assertThat(care["careThinMinutes"]).isEqualTo(15.0)

        @Suppress("UNCHECKED_CAST")
        val sleep = updated["sleep"] as Map<String, Any>
        assertThat(sleep["restlessHighEnabled"]).isEqualTo(false)
    }

    @Test
    fun `given jose has custom policy, when PUT maria, then maria independent of jose`() {
        putMap("/api/v1/insights/policies/jose", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighEnabled", false) })
        ))
        putMap("/api/v1/insights/policies/maria", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply {
                put("restlessHighEnabled", true)
                put("restlessHighThreshold", 0.40)
            }),
            "bathroom" to (fullBathroom.toMutableMap().apply { put("bathroomNightEnabled", false) })
        ))

        val maria = getMap("/api/v1/insights/policies/maria")
        assertThat(maria["residentId"]).isEqualTo("maria")

        @Suppress("UNCHECKED_CAST")
        val mariaSleep = maria["sleep"] as Map<String, Any>
        assertThat(mariaSleep["restlessHighEnabled"]).isEqualTo(true)
        assertThat(mariaSleep["restlessHighThreshold"]).isEqualTo(0.40)

        @Suppress("UNCHECKED_CAST")
        val mariaBathroom = maria["bathroom"] as Map<String, Any>
        assertThat(mariaBathroom["bathroomNightEnabled"]).isEqualTo(false)

        val jose = getMap("/api/v1/insights/policies/jose")
        assertThat(jose["residentId"]).isEqualTo("jose")

        @Suppress("UNCHECKED_CAST")
        val joseSleep = jose["sleep"] as Map<String, Any>
        assertThat(joseSleep["restlessHighEnabled"]).isEqualTo(false)
    }

    // ─── Reset ────────────────────────────────────────────────────────

    @Test
    fun `given jose has custom policy, when PUT reset, then falls back to default`() {
        putMap("/api/v1/insights/policies/jose", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighEnabled", false) }),
            "care" to (fullCare.toMutableMap().apply { put("careThinEnabled", false) })
        ))

        val reset = putMap("/api/v1/insights/policies/jose/reset", emptyMap())

        assertThat(reset["default"]).isEqualTo(true)
        assertThat(reset["residentId"]).isNull()
    }

    @Test
    fun `given jose was reset, when GET jose, then returns default values`() {
        putMap("/api/v1/insights/policies/jose", mapOf(
            "care" to (fullCare.toMutableMap().apply { put("careThinEnabled", false) })
        ))
        putMap("/api/v1/insights/policies/jose/reset", emptyMap())

        val policy = getMap("/api/v1/insights/policies/jose")

        assertThat(policy["default"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val care = policy["care"] as Map<String, Any>
        assertThat(care["careThinEnabled"]).isEqualTo(true)
    }

    // ─── Cascade Integrity ────────────────────────────────────────────

    @Test
    fun `given jose has custom threshold, when default updates, then jose unchanged`() {
        putMap("/api/v1/insights/policies/jose", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighThreshold", 0.20) })
        ))
        putMap("/api/v1/insights/policies/default", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighThreshold", 0.50) })
        ))

        val jose = getMap("/api/v1/insights/policies/jose")

        @Suppress("UNCHECKED_CAST")
        val sleep = jose["sleep"] as Map<String, Any>
        assertThat(sleep["restlessHighThreshold"]).isEqualTo(0.20)
    }

    @Test
    fun `given jose has custom threshold and default has different, when GET both, then each has its own`() {
        putMap("/api/v1/insights/policies/jose", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighThreshold", 0.20) })
        ))
        putMap("/api/v1/insights/policies/default", mapOf(
            "sleep" to (fullSleep.toMutableMap().apply { put("restlessHighThreshold", 0.50) })
        ))

        val jose = getMap("/api/v1/insights/policies/jose")

        @Suppress("UNCHECKED_CAST")
        val joseSleep = jose["sleep"] as Map<String, Any>
        assertThat(joseSleep["restlessHighThreshold"]).isEqualTo(0.20)

        val default = getMap("/api/v1/insights/policies/default")

        @Suppress("UNCHECKED_CAST")
        val defaultSleep = default["sleep"] as Map<String, Any>
        assertThat(defaultSleep["restlessHighThreshold"]).isEqualTo(0.50)
    }

    @Test
    fun `given pedro has no custom policy, when GET pedro, then shares default`() {
        val policy = getMap("/api/v1/insights/policies/pedro")

        assertThat(policy["default"]).isEqualTo(true)
        assertThat(policy["residentId"]).isNull()
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun getMap(path: String): Map<String, Any> = get(path) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun putMap(path: String, body: Map<String, Any?>): Map<String, Any> {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val request = HttpEntity(body, headers)
        val response = client.exchange("$baseUrl$path", HttpMethod.PUT, request, String::class.java)
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(response.body, Map::class.java) as Map<String, Any>
    }
}
