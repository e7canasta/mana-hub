package com.hub.integration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hub.integration")
data class IntegrationProperties(
    val sceneEvents: SceneEventsProperties = SceneEventsProperties(),
) {
    data class SceneEventsProperties(
        val ignore: Set<String> = emptySet(),
    )
}
