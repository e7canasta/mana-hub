package com.hub.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpisodeLifecycleTest : IntegrationTestBase() {

    @BeforeEach
    fun setUp() {
        cleanDatabase()
    }

    @Test
    fun `create and acknowledge episode`() {
        val created = post("/api/v1/episodes", mapOf(
            "residentId" to "jose",
            "bedId" to "bed-4",
            "severity" to "CRITICAL",
            "title" to "Caída detectada",
            "detail" to "Residente cayó al salir de la cama",
            "occurredAt" to "2026-08-31T02:30:00Z"
        )) as Map<String, Any>
        val episodeId = created["id"] as String
        assertThat(created["status"]).isEqualTo("PENDING")

        val acked = post("/api/v1/episodes/$episodeId/acknowledge", mapOf("actorId" to "nurse_1"))
        assertThat(acked).isNotNull
    }

    @Test
    fun `resolve episode through the semantic endpoint`() {
        val created = post("/api/v1/episodes", mapOf(
            "residentId" to "jose",
            "bedId" to "bed-4",
            "severity" to "WARNING",
            "title" to "Resolución test",
            "occurredAt" to "2026-08-31T03:00:00Z"
        )) as Map<String, Any>
        val episodeId = created["id"] as String

        val resolved = post(
            "/api/v1/episodes/$episodeId/resolved",
            mapOf("staffMemberId" to "staff-nurse-1"),
        ) as Map<String, Any>
        assertThat(resolved["status"]).isEqualTo("RESOLVED")

        val retried = post(
            "/api/v1/episodes/$episodeId/resolved",
            mapOf("staffMemberId" to "staff-nurse-1"),
        ) as Map<String, Any>
        assertThat(retried["status"]).isEqualTo("RESOLVED")
    }

    @Test
    fun `episode note via panel`() {
        val created = post("/api/v1/episodes", mapOf(
            "residentId" to "jose",
            "bedId" to "bed-4",
            "severity" to "INFO",
            "title" to "Note test",
            "occurredAt" to "2026-08-31T04:00:00Z"
        )) as Map<String, Any>
        val episodeId = created["id"] as String

        val note = post("/api/v1/panel/episodes/$episodeId/notes", mapOf(
            "kind" to "CLINICAL_NOTE",
            "body" to "Residente estable",
            "authorId" to "nurse_1"
        )) as Map<String, Any>
        assertThat(note["body"]).isEqualTo("Residente estable")
    }

    @Test
    fun `resident note via panel`() {
        val note = post("/api/v1/panel/residents/jose/notes", mapOf(
            "kind" to "CARE",
            "body" to "PA 140/90",
            "authorId" to "nurse_1"
        )) as Map<String, Any>
        assertThat(note["body"]).isEqualTo("PA 140/90")
    }
}
