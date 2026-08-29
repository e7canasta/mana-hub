package com.hub.policy.domain.model

@JvmInline
value class TemplateId(val value: String) {
    companion object {
        fun from(value: String): TemplateId = TemplateId(value)
    }
}
