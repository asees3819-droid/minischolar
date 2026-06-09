package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewResearchScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: MainViewModel = viewModel()
      val isDarkMode by viewModel.isDarkMode.collectAsState()
      val currentScreen by viewModel.currentScreen.collectAsState()

      MyApplicationTheme(darkTheme = isDarkMode) {
        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
              fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenNavigator"
          ) { screen ->
            when (screen) {
              is AppScreen.Dashboard -> {
                DashboardScreen(viewModel = viewModel)
              }
              is AppScreen.CreateResearch -> {
                NewResearchScreen(viewModel = viewModel)
              }
              is AppScreen.TrendMonitor -> {
                com.example.ui.screens.TrendMonitorScreen(viewModel = viewModel)
              }
              is AppScreen.ProjectWorkspace -> {
                ProjectDetailScreen(viewModel = viewModel)
              }
            }
          }
        }
      }
    }
  }
}
