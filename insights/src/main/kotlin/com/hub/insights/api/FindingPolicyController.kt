package com.hub.insights.api

import com.hub.insights.application.FindingPolicyService
import com.hub.insights.domain.find.BathroomPolicy
import com.hub.insights.domain.find.CarePolicy
import com.hub.insights.domain.find.FindingPolicy
import com.hub.insights.domain.find.SleepPolicy
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/insights/policies")
class FindingPolicyController(
    private val policyService: FindingPolicyService,
) {

    @GetMapping("/default")
    fun getDefault(): FindingPolicy =
        policyService.getDefault()

    @PutMapping("/default")
    fun updateDefault(
        @RequestBody body: FindingPolicyUpdateRequest,
    ): FindingPolicy = policyService.updateDefault(
        sleep = body.sleep,
        care = body.care,
        bathroom = body.bathroom,
    )

    @GetMapping("/{residentId}")
    fun getForResident(@PathVariable residentId: String): FindingPolicy =
        policyService.getForResident(residentId)

    @PutMapping("/{residentId}")
    fun updateForResident(
        @PathVariable residentId: String,
        @RequestBody body: FindingPolicyUpdateRequest,
    ): FindingPolicy = policyService.updateForResident(
        residentId = residentId,
        sleep = body.sleep,
        care = body.care,
        bathroom = body.bathroom,
    )

    @PutMapping("/{residentId}/reset")
    fun resetForResident(@PathVariable residentId: String): FindingPolicy =
        policyService.resetForResident(residentId)
}

data class FindingPolicyUpdateRequest(
    val sleep: SleepPolicy? = null,
    val care: CarePolicy? = null,
    val bathroom: BathroomPolicy? = null,
)
