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
import com.ovea_y.stabilitytool.subui.CpuCategoryPage
import com.ovea_y.stabilitytool.subui.CrashCategoryPage
import com.ovea_y.stabilitytool.subui.DiskCategoryPage
import com.ovea_y.stabilitytool.subui.MemoryCategoryPage
import com.ovea_y.stabilitytool.subui.NetworkCategoryPage
import com.ovea_y.stabilitytool.subui.PowerCategoryPage
import com.ovea_y.stabilitytool.subui.ResourceLeakCategoryPage
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme
import com.ovea_y.stabilitytool.utils.PermissionUtils

class MainActivity : ComponentActivity() {
    companion object {
        // 用于Activity泄漏测试的静态引用
        private var instance: MainActivity? = null
        
        fun getInstance(): MainActivity? {
            return instance
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置静态引用，用于Activity泄漏测试
        instance = this

        // 重启后自动触发查询（需主线程同步调用）
        val prefs = getSharedPreferences("anr_config", MODE_PRIVATE)
        if (prefs.getBoolean("onIndexAnr", false) && prefs.getBoolean("block_oncreate_index", false)) {
            prefs
                .edit()
                .putBoolean("block_oncreate_index", false)
                .putBoolean("onIndexAnr", false)
                .commit()
        } else {
            prefs
                .edit()
                .putBoolean("onIndexAnr", true)
                .commit()
        }
        if (prefs.getBoolean("block_oncreate_index", false)) {
            Thread.sleep(10000)
            prefs.edit().putBoolean("block_oncreate_index", false).commit() // 清除标记
        }
        
        // 请求必要的权限
        if (!PermissionUtils.hasAllPermissions(this)) {
            PermissionUtils.requestAllPermissions(this)
        }
        
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
                    composable("crash") { CrashCategoryPage(navController) }
                    composable("cpu") { CpuCategoryPage(navController) }
                    composable("disk") { DiskCategoryPage(navController) }
                    composable("memory") { MemoryCategoryPage(navController) }
                    composable("network") { NetworkCategoryPage(navController) }
                    composable("power") { PowerCategoryPage(navController) }
                    composable("resource_leak") { ResourceLeakCategoryPage(navController) }
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.handlePermissionResult(requestCode, permissions, grantResults)
    }


}
