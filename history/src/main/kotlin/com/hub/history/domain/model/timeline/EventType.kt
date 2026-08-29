package com.hub.history.domain.model.timeline

enum class EventType {
    OPENED,      // Episodio abierto (sentinel)
    ESCALATED,   // Severidad subió (sentinel)
    UMBRELLA,    // Movimiento contextual (scene)
    NOTIFIED,    // Alerta enviada al staff (harbor)
    RESPONDED,   // Staff respondió (harbor)
    STAFF_ARRIVED, // Staff llegó al cuarto (harbor/scene)
    RECOVERY,    // Residente volvió a seguridad (sentinel)
    CLOSED,      // Episodio cerrado (sentinel)
}
