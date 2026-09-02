package com.hub.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResidentRailProjectionTest : IntegrationTestBase() {

    @BeforeEach
    fun setUp() {
        cleanDatabase()
    }

    @Test
    fun `resident-rail returns list with seeded residents`() {
        @Suppress("UNCHECKED_CAST")
        val rail = getList("/api/v1/views/resident-rail") as List<Map<String, Any>>

        assertThat(rail).hasSizeGreaterThanOrEqualTo(2)

        val ids = rail.map { it["id"] as String }
        assertThat(ids).contains("jose", "maria")
    }

    @Test
    fun `jose has correct location in resident-rail`() {
        @Suppress("UNCHECKED_CAST")
        val rail = getList("/api/v1/views/resident-rail") as List<Map<String, Any>>
        val jose = rail.first { it["id"] == "jose" }

        assertThat(jose["fullName"]).isEqualTo("José García")

        @Suppress("UNCHECKED_CAST")
        val location = jose["location"] as? Map<String, Any>
        assertThat(location).isNotNull
        assertThat(location!!["bedLabel"]).isEqualTo("Cama A")
        assertThat(location["roomNumber"]).isEqualTo("301")
        assertThat(location["wingName"]).isEqualTo("Ala Norte")
    }

    @Test
    fun `maria has correct location in resident-rail`() {
        @Suppress("UNCHECKED_CAST")
        val rail = getList("/api/v1/views/resident-rail") as List<Map<String, Any>>
        val maria = rail.first { it["id"] == "maria" }

        assertThat(maria["fullName"]).isEqualTo("María García")

        @Suppress("UNCHECKED_CAST")
        val location = maria["location"] as? Map<String, Any>
        assertThat(location).isNotNull
        assertThat(location!!["bedLabel"]).isEqualTo("Cama B")
        assertThat(location["roomNumber"]).isEqualTo("302")
        assertThat(location["wingName"]).isEqualTo("Ala Norte")
    }

    @Test
    fun `current state is null when no bed state seeded`() {
        @Suppress("UNCHECKED_CAST")
        val rail = getList("/api/v1/views/resident-rail") as List<Map<String, Any>>
        val jose = rail.first { it["id"] == "jose" }

        assertThat(jose["currentState"]).isNull()
    }

    @Test
    fun `each rail item has all expected fields`() {
        @Suppress("UNCHECKED_CAST")
        val rail = getList("/api/v1/views/resident-rail") as List<Map<String, Any>>

        rail.forEach { item ->
            assertThat(item).containsKeys("id", "fullName", "location")
            assertThat(item["id"]).isNotNull
            assertThat(item["fullName"]).isNotNull

            @Suppress("UNCHECKED_CAST")
            val location = item["location"] as? Map<String, Any>
            if (location != null) {
                assertThat(location).containsKeys("bedLabel", "roomNumber", "wingName")
            }
        }
    }
}
