package com.hub.insights.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate

/**
 * Insights API — integración punta a punta.
 *
 * Patrón: SpringBootTest con Puerto random, HTTP real.
 * No mockeamos nada — el endpoint episodeResolved no depende de nadie.
 * Los endpoints que dependen del SOR los testamos cuando tengamos
 * el SOR levantado en el stack de integración completo.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "insights.hub-url=http://localhost:1",
        "insights.baseline-min-days=7",
        "insights.timezone=America/Argentina/Buenos_Aires",
    ],
)
@ActiveProfiles("test")
class InsightsApiIntegrationTest {

    @Value("\${local.server.port}")
    lateinit var port: String

    val baseUrl: String get() = "http://localhost:$port"

    val client: RestTemplate by lazy { RestTemplate() }

    @Nested
    inner class `POST episodes resolved endpoint` {

        @Test
        fun `auto-recovery returns EPISODE_SELF_RECOVERY`() {
            val response = postEpisodesResolved(
                residentId = "jose",
                episodeId = "ep-001",
                selfRecovery = true,
                durationMinutes = 17,
            )
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
            assertThat(response.body?.get("residentId")).isEqualTo("jose")
            assertThat(response.body?.get("episodeId")).isEqualTo("ep-001")
            @Suppress("UNCHECKED_CAST")
            val recs = response.body?.get("recommendations") as List<Map<String, Any>>
            assertThat(recs).hasSize(1)
            assertThat(recs[0]["code"]).isEqualTo("EPISODE_SELF_RECOVERY")
        }

        @Test
        fun `staff closure returns EPISODE_STAFF_CLOSED`() {
            val response = postEpisodesResolved(
                residentId = "maria",
                selfRecovery = false,
                durationMinutes = 25,
            )
            @Suppress("UNCHECKED_CAST")
            val recs = response.body?.get("recommendations") as List<Map<String, Any>>
            assertThat(recs[0]["code"]).isEqualTo("EPISODE_STAFF_CLOSED")
        }

        @Test
        fun `missing episodeId defaults to empty string`() {
            val response = postEpisodesResolved(
                residentId = "jose",
                selfRecovery = true,
            )
            assertThat(response.body?.get("episodeId")).isEqualTo("")
        }

        @Test
        fun `response contains residentId and recommendations`() {
            val response = postEpisodesResolved(
                residentId = "test-resident",
                selfRecovery = true,
            )
            @Suppress("UNCHECKED_CAST")
            val body = response.body as Map<String, Any>
            assertThat(body).containsKey("residentId")
            assertThat(body).containsKey("episodeId")
            assertThat(body).containsKey("recommendations")
        }
    }

    // Los endpoints GET (sleep, care, mobility, bathroom, briefing, report)
    // dependen del SOR via HubClient. Se testean con el stack completo
    // cuando el SOR está levantado en integración.
    // Ver: bootstrap/src/test/kotlin/com/hub/integration/ApiSmokeTest.kt

    private fun postEpisodesResolved(
        residentId: String,
        episodeId: String? = null,
        selfRecovery: Boolean,
        durationMinutes: Int? = null,
    ): ResponseEntity<Map<*, *>> {
        val body = mutableMapOf<String, Any>(
            "residentId" to residentId,
            "selfRecovery" to selfRecovery,
        )
        episodeId?.let { body["episodeId"] = it }
        durationMinutes?.let { body["durationMinutes"] = it }
        return client.postForEntity(
            "$baseUrl/internal/v1/insights/episodes/resolved",
            body,
            Map::class.java,
        )
    }
}
