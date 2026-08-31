package com.hub.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApiSmokeTest : IntegrationTestBase() {

    @BeforeEach
    fun setUp() {
        cleanDatabase()
    }

    @Test
    fun `health endpoint returns UP`() {
        val response = client.getForEntity("$baseUrl/actuator/health", Map::class.java)
        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        assertThat(response.body?.get("status")).isEqualTo("UP")
    }

    @Test
    fun `list facilities returns seeded facility`() {
        val facilities = getList("/api/v1/facilities")
        assertThat(facilities).isNotEmpty
        @Suppress("UNCHECKED_CAST")
        val facility = facilities[0] as Map<String, Any>
        assertThat(facility["name"]).isEqualTo("Residencia Los Robles")
    }

    @Test
    fun `get facility tree returns wings`() {
        val tree = get("/api/v1/facilities/fac-001/tree")
        assertThat(tree).isNotNull
    }

    @Test
    fun `list residents returns seeded residents`() {
        val residents = getList("/api/v1/residents")
        assertThat(residents.size).isGreaterThanOrEqualTo(2)
    }

    @Test
    fun `get resident by id returns jose`() {
        val jose = get("/api/v1/residents/jose") as Map<*, *>
        assertThat(jose["fullName"]).isEqualTo("José García")
    }

    @Test
    fun `list episodes is empty for fresh resident`() {
        val episodes = getList("/api/v1/episodes?residentId=jose")
        assertThat(episodes).isEmpty()
    }

    @Test
    fun `create episode then list it`() {
        val created = post("/api/v1/episodes", mapOf(
            "residentId" to "jose",
            "bedId" to "bed-4",
            "severity" to "WARNING",
            "title" to "Test episode",
            "occurredAt" to "2026-08-31T10:00:00Z"
        )) as Map<String, Any>
        assertThat(created["id"]).isNotNull

        val episodes = getList("/api/v1/episodes?residentId=jose")
        assertThat(episodes).isNotEmpty
    }

    @Test
    fun `alarm profile for jose exists`() {
        val profile = get("/api/v1/alarm-presets/jose")
        assertThat(profile).isNotNull
    }

    @Test
    fun `panel residents endpoint works`() {
        val residents = getList("/api/v1/panel/residents")
        assertThat(residents).isNotEmpty
    }

    @Test
    fun `panel episode feed endpoint works`() {
        val feed = get("/api/v1/panel/episodes")
        assertThat(feed).isNotNull
    }
}
