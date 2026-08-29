package com.manahive.messaging

/**
 * The subject taxonomy of the bus. Version is part of the subject: a breaking
 * change is a NEW subject, and old consumers keep working until retired.
 *
 * Shared header — copied from mana-hive/platform/messaging/Subjects.kt:11
 * Adapted to use String IDs to avoid kernel dependency in mana-hub.
 */
public object Subjects {
    public fun perceptionObservation(bedId: String): String = "perception.observation.v1.$bedId"
    public fun sceneEvent(bedId: String): String = "scene.fact.v1.$bedId"
    public fun sentinelSignal(bedId: String): String = "sentinel.signal.v1.$bedId"
    public fun alarmEvent(alertId: String): String = "alarm.event.v1.$alertId"
    public fun effectiveRules(residentId: String): String = "hub.policy.effective-rules.v1.$residentId"
    public fun policyChangeDetected(): String = "hub.policy.change.v1"
    public fun recordingCommand(bedId: String): String = "recorder.command.v1.$bedId"
    public fun evidenceRecord(bedId: String): String = "evidence.record.v1.$bedId"

    public const val CENSUS_SNAPSHOT: String = "hub.census.snapshot.v1"

    public const val PERCEPTION_WILDCARD: String = "perception.observation.v1.>"
    public const val SCENE_WILDCARD: String = "scene.fact.v1.>"
    public const val SENTINEL_WILDCARD: String = "sentinel.signal.v1.>"
    public const val ALARM_WILDCARD: String = "alarm.event.v1.>"
    public const val POLICY_WILDCARD: String = "hub.policy.>"
    public const val RECORDER_WILDCARD: String = "recorder.command.v1.>"
    public const val EVIDENCE_WILDCARD: String = "evidence.record.v1.>"
}
