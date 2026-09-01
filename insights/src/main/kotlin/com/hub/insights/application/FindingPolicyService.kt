package com.hub.insights.application

import com.hub.insights.domain.find.BathroomPolicy
import com.hub.insights.domain.find.CarePolicy
import com.hub.insights.domain.find.FindingPolicy
import com.hub.insights.domain.find.SleepPolicy
import com.hub.insights.domain.repository.FindingPolicyRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FindingPolicyService(
    private val repository: FindingPolicyRepository,
) {

    /**
     * Cascada: residente → default → crear default.
     *
     * Si el residente tiene política propia, la retorna.
     * Si no tiene, busca la default (isDefault = true).
     * Si no hay default, crea una con todos los enabled=true.
     */
    fun getForResident(residentId: String): FindingPolicy {
        return repository.findByResidentId(residentId)
            ?: repository.findDefault()
            ?: createDefault()
    }

    fun getDefault(): FindingPolicy {
        return repository.findDefault() ?: createDefault()
    }

    fun updateDefault(
        sleep: SleepPolicy? = null,
        care: CarePolicy? = null,
        bathroom: BathroomPolicy? = null,
    ): FindingPolicy {
        val current = getDefault()
        val updated = current.copy(
            sleep = sleep ?: current.sleep,
            care = care ?: current.care,
            bathroom = bathroom ?: current.bathroom,
        )
        return repository.save(updated)
    }

    fun updateForResident(
        residentId: String,
        sleep: SleepPolicy? = null,
        care: CarePolicy? = null,
        bathroom: BathroomPolicy? = null,
    ): FindingPolicy {
        val current = repository.findByResidentId(residentId)
            ?: FindingPolicy(
                id = UUID.randomUUID().toString(),
                residentId = residentId,
            )
        val updated = current.copy(
            sleep = sleep ?: current.sleep,
            care = care ?: current.care,
            bathroom = bathroom ?: current.bathroom,
        )
        return repository.save(updated)
    }

    fun resetForResident(residentId: String): FindingPolicy {
        repository.deleteByResidentId(residentId)
        return getForResident(residentId)
    }

    private fun createDefault(): FindingPolicy {
        val policy = FindingPolicy(
            id = UUID.randomUUID().toString(),
            isDefault = true,
        )
        return repository.save(policy)
    }
}
