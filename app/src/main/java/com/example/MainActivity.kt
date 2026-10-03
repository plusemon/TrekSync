package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.screens.TrekMainScreen
import com.example.ui.theme.TrekSyncTheme
import com.example.ui.viewmodel.TrekViewModel
import com.example.util.BatteryOptimizationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: TrekViewModel by viewModels {
        TrekViewModel.Factory(application as TrekSyncApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and prompt for battery optimization exemption to prevent background network sleep
        promptBatteryOptimizationExemptionIfRecommended()

        setContent {
            TrekSyncTheme {
                TrekMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    /**
     * Helper to prompt for battery optimization exemptions (Doze mode bypass).
     */
    fun promptBatteryOptimizationExemptionIfRecommended() {
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            // Can be triggered on user action or startup for outdoor group tracking
            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
        }
    }

    /**
     * Direct method to request battery optimization exemption from UI callbacks.
     */
    fun requestBatteryOptimizationExemption() {
        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        return BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
    }
}
