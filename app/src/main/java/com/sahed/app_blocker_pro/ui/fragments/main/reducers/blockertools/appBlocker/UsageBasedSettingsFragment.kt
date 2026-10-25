package com.sahed.app_blocker_pro.ui.fragments.main.reducers.blockertools.appBlocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.sahed.app_blocker_pro.data.models.AppUsageConfig
import com.sahed.app_blocker_pro.databinding.FragmentAppBlockerUsageSettingsBinding
import com.sahed.app_blocker_pro.ui.fragments.main.reducers.blockertools.shared.BaseUsageSettingsFragment

class UsageBasedSettingsFragment : BaseUsageSettingsFragment() {

    companion object {
        const val FRAGMENT_ID = "UsageBasedSettingsBottomSheet"
    }

    private val viewModel: AppBlockerSettingViewModel by activityViewModels()

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): View =
        FragmentAppBlockerUsageSettingsBinding.inflate(inflater, container, false).root

    override fun loadUsageConfig(): AppUsageConfig = viewModel.currentUsageConfig

    override fun saveUsageConfig(config: AppUsageConfig) {
        viewModel.currentUsageConfig = config
    }
}
