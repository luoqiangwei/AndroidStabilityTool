package com.ovea_y.stabilitytool.subui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ovea_y.stabilitytool.CommonButton
import com.ovea_y.stabilitytool.R
import com.ovea_y.stabilitytool.service.PowerTestService
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme
import kotlinx.coroutines.launch

@Composable
fun PowerCategoryPage(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val powerTestService = remember { PowerTestService() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val powerCommonTip = stringResource(R.string.power_common_tip)
    val stoppingTestsMessage = stringResource(R.string.stopping_tests)
    val context = LocalContext.current
    
    // 在组件销毁时清理资源
    DisposableEffect(Unit) {
        onDispose {
            powerTestService.onDestroy()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            Column(modifier = Modifier
                .padding(contentPadding)
                .padding(all = GlobalCommonPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)) {
                Text(
                    stringResource(id = R.string.power_test),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center)
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                
                Text(
                    text = stringResource(R.string.power_test_warning),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                Row() {
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize(0.5f)) {

                        // CPU高负载耗电测试
                        CommonButton(text = stringResource(R.string.power_cpu_high_load)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(powerCommonTip)
                            }
                            powerTestService.causeCpuHighLoadPowerDrain(300)
                        }

                        // 屏幕常亮耗电测试
                        CommonButton(text = stringResource(R.string.power_screen_on)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(powerCommonTip)
                            }
                            powerTestService.causeScreenOnPowerDrain(context, 300)
                        }

                        // GPS持续定位耗电测试
                        CommonButton(text = stringResource(R.string.power_gps)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(powerCommonTip)
                            }
                            powerTestService.causeGpsPowerDrain(context, 300)
                        }

                        // 传感器持续监听耗电测试
                        CommonButton(text = stringResource(R.string.power_sensors)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(powerCommonTip)
                            }
                            powerTestService.causeSensorPowerDrain(context, 300)
                        }
                    }
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize()) {

                        // 网络持续传输耗电测试
                        CommonButton(text = stringResource(R.string.power_network)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(powerCommonTip)
                            }
                            powerTestService.causeNetworkPowerDrain(context, 300)
                        }

                        // 混合场景耗电测试
                        CommonButton(text = stringResource(R.string.power_mixed_scenario)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(powerCommonTip)
                            }
                            powerTestService.causeMixedScenarioPowerDrain(context, 300)
                        }

                        // 后台持续运行耗电测试
                        CommonButton(text = stringResource(R.string.power_background)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(powerCommonTip)
                            }
                            powerTestService.causeBackgroundPowerDrain(300)
                        }

                        // 停止所有测试
                        CommonButton(text = stringResource(R.string.stop_all_tests)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(stoppingTestsMessage)
                            }
                            powerTestService.stopAllTests()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PowerCategoryPagePreview() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "power") {
        composable("power") { PowerCategoryPage(navController) }
    }

    StabilityToolTheme {
        PowerCategoryPage(navController)
    }
} 