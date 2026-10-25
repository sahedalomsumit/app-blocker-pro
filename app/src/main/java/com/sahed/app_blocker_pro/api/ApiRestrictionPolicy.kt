package com.sahed.app_blocker_pro.api

import com.sahed.app_blocker_pro.Constants
import com.sahed.app_blocker_pro.data.models.AppBlockerWarningScreenConfig
import com.sahed.app_blocker_pro.data.models.Settings

internal object ApiRestrictionPolicy {
    fun canReplaceFocus(settings: Settings, nowMs: Long): Boolean {
        val (groupId, endTime) = settings.activeManualFocusGroupId
        return groupId == null || endTime <= nowMs ||
            settings.manualFocusGroups.firstOrNull { it.groupId == groupId }?.exitable == true
    }

    fun warningStillCurrent(
        settings: Settings, mode: Int, target: String, expected: AppBlockerWarningScreenConfig
    ): Boolean {
        if (expected.externalUnlockCondition == null) return false
        val current = when (mode) {
            Constants.WARNING_SCREEN_MODE_APP_BLOCKER -> settings.blockedAppGroups
                .firstOrNull { it.id == target && it.isActive }?.warningScreenConfig
            Constants.WARNING_SCREEN_MODE_KEYWORD_BLOCKER -> if (settings.keywordBlockerConfig.isActive) {
                settings.keywordBlockerConfig.keywordGroups
                    .firstOrNull { it.id == target && it.isActive }?.warningScreenConfig
            } else null
            Constants.WARNING_SCREEN_MODE_VIEW_BLOCKER -> settings.reelBlockerConfig
                .takeIf { it.isActive }?.warningScreenConfig
            else -> null
        }
        return current == expected
    }
}
