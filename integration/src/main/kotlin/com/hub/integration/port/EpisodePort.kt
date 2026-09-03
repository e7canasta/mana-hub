package com.hub.integration.port

// Re-export from shared-kernel to avoid duplication
// All port classes are defined in com.hub.shared.domain.port
typealias EpisodePort = com.hub.shared.domain.port.EpisodePort
typealias CreateEpisodePortRequest = com.hub.shared.domain.port.CreateEpisodePortRequest
typealias UpdateEpisodePortRequest = com.hub.shared.domain.port.UpdateEpisodePortRequest
typealias EpisodePortResponse = com.hub.shared.domain.port.EpisodePortResponse
typealias EpisodePortModel = com.hub.shared.domain.port.EpisodePortModel
