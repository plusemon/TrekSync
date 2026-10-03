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

class MainActivity : ComponentActivity() {

    private val viewModel: TrekViewModel by viewModels {
        TrekViewModel.Factory(application as TrekSyncApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrekSyncTheme {
                TrekMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
