package com.hub.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResidentLifecycleTest : IntegrationTestBase() {

    @BeforeEach
    fun setUp() {
        cleanDatabase()
    }

    @Test
    fun `list all residents returns seeded residents with location`() {
        @Suppress("UNCHECKED_CAST")
        val residents = getList("/api/v1/residents") as List<Map<String, Any>>

        assertThat(residents).hasSizeGreaterThanOrEqualTo(2)

        val jose = residents.first { it["id"] == "jose" }
        assertThat(jose["fullName"]).isEqualTo("José García")
        assertThat(jose["status"]).isEqualTo("ACTIVE")
        assertThat(jose["isDischarged"]).isEqualTo(false)
        assertThat(jose["birthDate"]).isEqualTo("1942-03-15")
        assertThat(jose["admissionDate"]).isEqualTo("2024-01-15")

        @Suppress("UNCHECKED_CAST")
        val location = jose["location"] as? Map<String, Any>
        assertThat(location).isNotNull
        assertThat(location!!["bedLabel"]).isEqualTo("Cama A")
        assertThat(location["roomNumber"]).isEqualTo("301")
        assertThat(location["wingName"]).isEqualTo("Ala Norte")
    }

    @Test
    fun `get resident by id returns jose with location`() {
        @Suppress("UNCHECKED_CAST")
        val jose = get("/api/v1/residents/jose") as Map<String, Any>

        assertThat(jose["id"]).isEqualTo("jose")
        assertThat(jose["fullName"]).isEqualTo("José García")
        assertThat(jose["status"]).isEqualTo("ACTIVE")

        @Suppress("UNCHECKED_CAST")
        val location = jose["location"] as? Map<String, Any>
        assertThat(location).isNotNull
        assertThat(location!!["bedLabel"]).isEqualTo("Cama A")
        assertThat(location["roomNumber"]).isEqualTo("301")
        assertThat(location["wingName"]).isEqualTo("Ala Norte")
    }

    @Test
    fun `get resident by id returns maria with location`() {
        @Suppress("UNCHECKED_CAST")
        val maria = get("/api/v1/residents/maria") as Map<String, Any>

        assertThat(maria["id"]).isEqualTo("maria")
        assertThat(maria["fullName"]).isEqualTo("María García")
        assertThat(maria["status"]).isEqualTo("ACTIVE")

        @Suppress("UNCHECKED_CAST")
        val location = maria["location"] as? Map<String, Any>
        assertThat(location).isNotNull
        assertThat(location!!["bedLabel"]).isEqualTo("Cama B")
        assertThat(location["roomNumber"]).isEqualTo("302")
        assertThat(location["wingName"]).isEqualTo("Ala Norte")
    }

    @Test
    fun `resident response has all expected fields`() {
        @Suppress("UNCHECKED_CAST")
        val jose = get("/api/v1/residents/jose") as Map<String, Any>

        assertThat(jose).containsKeys(
            "id", "fullName", "birthDate", "admissionDate",
            "status", "isDischarged", "location"
        )
        assertThat(jose["isDischarged"]).isEqualTo(false)

        @Suppress("UNCHECKED_CAST")
        val location = jose["location"] as Map<String, Any>
        assertThat(location).containsKeys("bedLabel", "roomNumber", "wingName")
    }
}
