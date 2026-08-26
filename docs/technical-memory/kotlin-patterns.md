# Kotlin Patterns

## Context Parameters (Kotlin 2.4+)

```kotlin
context(logger: Logger, audit: AuditLog)
fun processEvent(event: Event) {
    logger.info("Processing ${event.id}")
    audit.record(event)
}
```

## Sealed Interfaces

```kotlin
sealed interface EpisodeState {
    data object Pending : EpisodeState
    data object Acknowledged : EpisodeState
    data class Resolved(val resolution: String) : EpisodeState
}
```

## Type-Safe Builders

```kotlin
@DslMarker
annotation class SurveillanceDsl

@SurveillanceDsl
class EpisodeBuilder {
    var severity: EpisodeSeverity = EpisodeSeverity.WARNING
    var title: String? = null
}

fun triggerEpisode(block: EpisodeBuilder.() -> Unit): Episode =
    EpisodeBuilder().apply(block).build()
```

## Contracts

```kotlin
inline fun <T> ensureNotNull(value: T?, block: (T) -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    if (value != null) block(value)
}
```

## Explicit Backing Fields

```kotlin
class Episode {
    val state: EpisodeState
        field = EpisodeState.Pending

    fun acknowledge() {
        state = EpisodeState.Acknowledged  // smart-cast
    }
}
```
