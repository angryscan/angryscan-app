package org.angryscan.app.db.models

enum class TaskState {
    LOADING,
    PENDING,
    SEARCHING,
    SCANNING,
    COMPLETED,
    FAILED,
    STOPPED
}
