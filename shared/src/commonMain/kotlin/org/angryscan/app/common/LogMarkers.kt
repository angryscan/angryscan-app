package org.angryscan.app.common

import io.github.oshai.kotlinlogging.Marker

enum class LogMarkers(val markerName: String): Marker {
    UserAction(markerName = "USER_ACTION");

    override fun getName(): String = this.markerName
}