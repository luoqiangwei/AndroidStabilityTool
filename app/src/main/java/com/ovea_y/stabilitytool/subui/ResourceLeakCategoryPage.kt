package com.ovea_y.stabilitytool.subui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun ResourceLeakCategoryPage(navController: NavHostController) {
    ResourceLeakCategoryUI(
        navController = navController,
        onBackPressed = { navController.popBackStack() }
    )
} 