package com.hub.insights.infrastructure.persistence

import com.hub.insights.domain.find.BathroomPolicy
import com.hub.insights.domain.find.CarePolicy
import com.hub.insights.domain.find.FindingPolicy
import com.hub.insights.domain.find.SleepPolicy
import com.hub.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "finding_policies")
class FindingPolicyEntity(

    @Id
    @Column(length = 36)
    val id: String,

    @Column(name = "resident_id", length = 36, unique = true)
    val residentId: String?,

    @Column(name = "is_default", nullable = false)
    val isDefault: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var sleep: SleepPolicy = SleepPolicy(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var care: CarePolicy = CarePolicy(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var bathroom: BathroomPolicy = BathroomPolicy(),

    @Version
    var version: Long = 0,

) : BaseEntity() {

    fun toDomain(): FindingPolicy = FindingPolicy(
        id = id,
        residentId = residentId,
        isDefault = isDefault,
        sleep = sleep,
        care = care,
        bathroom = bathroom,
        version = version,
    )

    companion object {
        fun fromDomain(domain: FindingPolicy): FindingPolicyEntity = FindingPolicyEntity(
            id = domain.id,
            residentId = domain.residentId,
            isDefault = domain.isDefault,
            sleep = domain.sleep,
            care = domain.care,
            bathroom = domain.bathroom,
            version = domain.version,
        )
    }
}
