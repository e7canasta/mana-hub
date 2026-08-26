package com.hub.evidence.domain.repository

import com.hub.evidence.domain.model.*
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId

interface EvidenceRepository {
    fun findById(id: EvidenceId): Evidence?
    fun findByBedId(bedId: BedId): List<Evidence>
    fun save(evidence: Evidence): Evidence
}

interface TimelineRepository {
    fun findById(id: EvidenceId): Timeline?
    fun save(timeline: Timeline): Timeline
}

interface ClipWindowRepository {
    fun findById(id: EvidenceId): ClipWindow?
    fun findOpenByBedId(bedId: BedId): ClipWindow?
    fun save(clipWindow: ClipWindow): ClipWindow
}
