package com.hub.observation.application.dto

data class StateCatalogEntry(
    val id: String,
    val label: String,
    val group: String,
    val pictogram: String
)

data class StateCatalogResponse(
    val states: List<StateCatalogEntry>,
    val roomStates: List<StateCatalogEntry>
)
