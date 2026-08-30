package com.hub.integration.domain.model

import java.time.Instant

/**
 * Resident profile — full version from mana-hive.
 *
 * Stored as immutable versions. Each version is complete, never a delta.
 * The raw_json preserves the exact format that mana-hive consumes.
 */
data class ResidentProfile(
    val id: String,
    val residentId: String,
    val profileId: String,
    val version: Int,
    val supersedes: Int?,
    val validFrom: Instant,
    val provenanceJson: String,
    val windowsJson: String,
    val subjectsJson: String,
    val rawJson: String,
    val createdAt: Instant = Instant.now(),
) {
    val isCurrent: Boolean get() = supersedes == null

    companion object {
        fun fromRawJson(rawJson: String, id: String = java.util.UUID.randomUUID().toString()): ResidentProfile {
            val tree = com.fasterxml.jackson.databind.ObjectMapper().readTree(rawJson)
            val validFromText = tree.path("validFrom").asText("")
            val validFrom = if (validFromText.isNotEmpty()) {
                java.time.Instant.parse(validFromText)
            } else {
                java.time.Instant.now()
            }
            return ResidentProfile(
                id = id,
                residentId = tree.path("residentId").asText("unknown"),
                profileId = tree.path("profileId").asText("unknown"),
                version = tree.path("version").asInt(1),
                supersedes = tree.path("supersedes").asInt(-1).let { if (it < 0) null else it },
                validFrom = validFrom,
                provenanceJson = tree.path("provenance").toString(),
                windowsJson = tree.path("windows").toString(),
                subjectsJson = tree.path("subjects").toString(),
                rawJson = rawJson,
            )
        }
    }
}
