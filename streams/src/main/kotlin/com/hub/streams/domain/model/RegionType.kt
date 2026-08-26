package com.hub.streams.domain.model

enum class RegionType {
    BATHROOM,
    HALLWAY,
    EXIT,
    BED,
    FURNITURE,
    PERSON,
    OBJECT;

    companion object {
        fun from(value: String): RegionType = when (value.lowercase()) {
            "bathroom" -> BATHROOM
            "hallway" -> HALLWAY
            "exit" -> EXIT
            "bed" -> BED
            "furniture" -> FURNITURE
            "person" -> PERSON
            "object" -> OBJECT
            else -> throw IllegalArgumentException("Unknown region type: $value")
        }
    }
}
