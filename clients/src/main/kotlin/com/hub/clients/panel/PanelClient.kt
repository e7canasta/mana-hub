package com.hub.clients.panel

import com.hub.clients.core.HttpApi
import com.hub.clients.core.PanelDsl

@PanelDsl
class PanelScope internal constructor(url: String) {

    private val http = HttpApi(url)

    val residents: ResidentScope = ResidentScope(http)
    val episodes: EpisodeScope = EpisodeScope(http)
    val preferences: PreferenceScope = PreferenceScope(http)
}

fun panel(url: String = "http://localhost:8080", block: PanelScope.() -> Unit): PanelScope =
    PanelScope(url).apply(block)
