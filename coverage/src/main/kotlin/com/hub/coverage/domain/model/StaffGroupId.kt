package com.hub.coverage.domain.model

import java.util.UUID

@JvmInline
value class StaffGroupId(val value: String) {
    companion object {
        fun from(value: String): StaffGroupId = StaffGroupId(value)
        fun random(): StaffGroupId = StaffGroupId(UUID.randomUUID().toString())
    }
}
