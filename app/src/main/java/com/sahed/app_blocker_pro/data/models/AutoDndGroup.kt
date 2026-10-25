package com.sahed.app_blocker_pro.data.models

import java.util.UUID

data class AutoDndGroup(
    val groupId: String = UUID.randomUUID().toString(),
    val groupName: String = "",
    val autoTurnOnDnd: Boolean = true,
    var timeConfig: AppTimeConfig = AppTimeConfig()
) {
    override fun toString(): String {
        return groupName
    }
}
