package com.hub.policy.infrastructure.persistence

import com.hub.policy.domain.model.PolicyOverride
import com.hub.policy.domain.repository.AlarmProfileOverrideRepository
import com.hub.shared.domain.BaseEntity
import com.hub.shared.domain.Identifier
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Entity
@Table(name = "alarm_profile_overrides")
class AlarmProfileOverrideEntity(
    @Id var id: String = "",
    @Column(name = "profile_version_id") var profileVersionId: String = "",
    @Column(name = "rule_id") var ruleId: String = "",
    @Column(name = "override_type") var overrideType: String = "",
    @Column(name = "state_kind") var stateKind: String? = null,
    @Column(name = "transition_key") var transitionKey: String? = null,
    @Column(name = "warning_after_minutes") var warningAfterMinutes: Int? = null,
    @Column(name = "alert_after_minutes") var alertAfterMinutes: Int? = null,
    @Column(name = "hysteresis_seconds") var hysteresisSeconds: Int? = null,
    @Column(name = "baseline_state") var baselineState: String? = null,
    @Column(name = "severity") var severity: String? = null,
    @Column(name = "closure_condition") var closureCondition: String? = null,
    /* La regla se mira y no habla. Ver `PolicyOverride.observeOnly`. */
    @Column(name = "observe_only") var observeOnly: Boolean? = null,
) : BaseEntity()

@Repository
interface AlarmProfileOverrideEntityRepository : JpaRepository<AlarmProfileOverrideEntity, String> {
    fun findByProfileVersionId(profileVersionId: String): List<AlarmProfileOverrideEntity>

    @Modifying
    @Transactional
    @Query("DELETE FROM AlarmProfileOverrideEntity e WHERE e.profileVersionId = :profileVersionId")
    fun deleteByProfileVersionId(profileVersionId: String)
}

@Repository
class AlarmProfileOverrideRepositoryAdapter(
    private val jpa: AlarmProfileOverrideEntityRepository
) : AlarmProfileOverrideRepository {

    override fun findByProfileVersionId(profileVersionId: String): List<PolicyOverride> =
        jpa.findByProfileVersionId(profileVersionId).map { it.toDomain() }

    override fun saveAll(overrides: List<PolicyOverride>, profileVersionId: String) {
        jpa.deleteByProfileVersionId(profileVersionId)
        jpa.flush()
        val entities = overrides.map { it.toEntity(profileVersionId) }
        jpa.saveAll(entities)
    }

    override fun deleteByProfileVersionId(profileVersionId: String) {
        jpa.deleteByProfileVersionId(profileVersionId)
    }

    private fun AlarmProfileOverrideEntity.toDomain(): PolicyOverride = when (overrideType) {
        "hysteresis" -> PolicyOverride.HysteresisOverride(
            id = Identifier(id),
            ruleId = ruleId,
            transitionKey = transitionKey ?: "",
            hysteresisSeconds = hysteresisSeconds ?: 0,
            severity = severity,
            closureCondition = closureCondition,
            observeOnly = observeOnly,
        )
        "dwell" -> PolicyOverride.DwellOverride(
            id = Identifier(id),
            ruleId = ruleId,
            stateKind = stateKind ?: "",
            warningAfterMinutes = warningAfterMinutes,
            alertAfterMinutes = alertAfterMinutes,
            severity = severity,
            closureCondition = closureCondition,
            observeOnly = observeOnly,
        )
        "comeback" -> PolicyOverride.ComeBackOverride(
            id = Identifier(id),
            ruleId = ruleId,
            baselineState = baselineState ?: "",
            warningAfterMinutes = warningAfterMinutes,
            alertAfterMinutes = alertAfterMinutes,
            severity = severity,
            closureCondition = closureCondition,
            observeOnly = observeOnly,
        )
        else -> throw IllegalArgumentException("Unknown override type: $overrideType")
    }

    private fun PolicyOverride.toEntity(profileVersionId: String): AlarmProfileOverrideEntity = when (this) {
        is PolicyOverride.HysteresisOverride -> AlarmProfileOverrideEntity(
            id = id.value,
            profileVersionId = profileVersionId,
            ruleId = ruleId,
            overrideType = "hysteresis",
            transitionKey = transitionKey,
            hysteresisSeconds = hysteresisSeconds,
            severity = severity,
            closureCondition = closureCondition,
            observeOnly = observeOnly,
        )
        is PolicyOverride.DwellOverride -> AlarmProfileOverrideEntity(
            id = id.value,
            profileVersionId = profileVersionId,
            ruleId = ruleId,
            overrideType = "dwell",
            stateKind = stateKind,
            warningAfterMinutes = warningAfterMinutes,
            alertAfterMinutes = alertAfterMinutes,
            severity = severity,
            closureCondition = closureCondition,
            observeOnly = observeOnly,
        )
        is PolicyOverride.ComeBackOverride -> AlarmProfileOverrideEntity(
            id = id.value,
            profileVersionId = profileVersionId,
            ruleId = ruleId,
            overrideType = "comeback",
            baselineState = baselineState,
            warningAfterMinutes = warningAfterMinutes,
            alertAfterMinutes = alertAfterMinutes,
            severity = severity,
            closureCondition = closureCondition,
            observeOnly = observeOnly,
        )
    }
}
