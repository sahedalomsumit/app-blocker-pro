package com.sahed.app_blocker_pro.data.models

/** A provider owned condition selected by the user in a warning configuration. */
data class ExternalUnlockCondition(
    val packageName: String = "",
    val id: String = "",
    val label: String = "",
    val activityName: String = "",
    val signingCertificates: List<String> = emptyList()
)
