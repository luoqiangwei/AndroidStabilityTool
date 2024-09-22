package com.ovea_y.stabilitytool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ovea_y.stabilitytool.subui.AnrCategoryPage
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StabilityToolTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { slideInVertically(
                        initialOffsetY = { -300 },
                        animationSpec = tween(durationMillis = 700)
                    ) + scaleIn(initialScale = 0.85f) },
                    exitTransition = { slideOutVertically(
                        targetOffsetY = { 300 },
                        animationSpec = tween(durationMillis = 100)
                    ) + fadeOut(animationSpec = tween(durationMillis = 0))
                    }
                ) {
                    composable("home") { HomePage(navController) }
                    composable("anr") { AnrCategoryPage(navController) }
                }
            }
        }
    }
}
