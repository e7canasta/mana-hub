package com.hub.shared.domain

data class BedLocation(
    val wingId: String? = null,
    val wingName: String? = null,
    val roomId: String? = null,
    val roomNumber: String? = null,
    val bedId: String? = null,
    val bedLabel: String? = null,
) {
    companion object {
        fun of(wingName: String?, roomNumber: String?, bedLabel: String?) =
            BedLocation(wingName = wingName, roomNumber = roomNumber, bedLabel = bedLabel)
    }
}
