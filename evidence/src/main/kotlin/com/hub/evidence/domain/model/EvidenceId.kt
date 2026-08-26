package com.hub.evidence.domain.model

import java.util.UUID

@JvmInline
value class EvidenceId(val value: String) {
    companion object {
        fun from(value: String): EvidenceId = EvidenceId(value)
        fun random(): EvidenceId = EvidenceId(UUID.randomUUID().toString())
    }
}
