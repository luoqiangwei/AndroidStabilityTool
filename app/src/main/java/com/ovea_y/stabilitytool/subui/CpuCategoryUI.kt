package com.ovea_y.stabilitytool.subui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ovea_y.stabilitytool.CommonButton
import com.ovea_y.stabilitytool.R
import com.ovea_y.stabilitytool.TestResultDisplay
import com.ovea_y.stabilitytool.rememberTestResultManager
import com.ovea_y.stabilitytool.service.CpuTestService
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme
import kotlinx.coroutines.launch

@Composable
fun CpuCategoryPage(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val cpuTestService = remember { CpuTestService() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val cpuCommonTip = stringResource(R.string.cpu_common_tip)
    val stoppingTestsMessage = stringResource(R.string.stopping_tests)
    
    // 测试结果管理器
    val testResultManager = rememberTestResultManager()
    
    // 日志状态
    val logState = remember { mutableStateOf("") }
    
    // 设置日志回调
    DisposableEffect(cpuTestService) {
        cpuTestService.setLogCallback(object : CpuTestService.LogCallback {
            override fun onLogUpdated(log: String) {
                logState.value = log
            }
            
            override fun onTestStarted(testName: String) {
                testResultManager.startTest(testName)
            }
            
            override fun onTestProgress(progress: Float, message: String) {
                testResultManager.updateProgress(progress, message)
            }
            
            override fun onTestCompleted(success: Boolean, message: String, details: String) {
                if (success) {
                    testResultManager.testSuccess(message, details)
                } else {
                    testResultManager.testError(message, details)
                }
            }
        })
        
        onDispose {
            cpuTestService.onDestroy()
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
                    stringResource(id = R.string.cpu_test),
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

                Row() {
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize(0.5f)) {

                        // 单核高负载测试
                        CommonButton(text = stringResource(R.string.cpu_single_core_high_load)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(cpuCommonTip)
                            }
                            cpuTestService.causeSingleCoreHighLoad(60)
                        }

                        // 多核高负载测试
                        CommonButton(text = stringResource(R.string.cpu_multi_core_high_load)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(cpuCommonTip)
                            }
                            cpuTestService.causeMultiCoreHighLoad(60)
                        }

                        // 频繁GC测试
                        CommonButton(text = stringResource(R.string.cpu_frequent_gc)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(cpuCommonTip)
                            }
                            cpuTestService.causeFrequentGC(60)
                        }
                    }
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize()) {

                        // 高频线程创建与销毁测试
                        CommonButton(text = stringResource(R.string.cpu_thread_churn)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(cpuCommonTip)
                            }
                            cpuTestService.causeFrequentThreadCreation(60)
                        }

                        // 素数计算测试
                        CommonButton(text = stringResource(R.string.cpu_prime_calculation)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(cpuCommonTip)
                            }
                            cpuTestService.causePrimeNumberCalculation(60)
                        }

                        // 温度升高测试
                        CommonButton(text = stringResource(R.string.cpu_temperature_rise)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(cpuCommonTip)
                            }
                            cpuTestService.causeTemperatureRise(300)
                        }

                        // 停止所有测试
                        CommonButton(text = stringResource(R.string.stop_all_tests)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(stoppingTestsMessage)
                            }
                            cpuTestService.stopAllTests()
                        }
                    }
                }
                
                // 测试结果显示
                Spacer(modifier = Modifier.height(16.dp))
                
                // 测试结果卡片
                if (testResultManager.testResult.value.title.isNotEmpty()) {
                    TestResultDisplay(
                        title = testResultManager.testResult.value.title,
                        status = testResultManager.testResult.value.status,
                        progress = testResultManager.testResult.value.progress,
                        message = testResultManager.testResult.value.message,
                        details = testResultManager.testResult.value.details
                    )
                }
                
                // 日志显示卡片
                Spacer(modifier = Modifier.height(8.dp))
                if (logState.value.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.test_logs),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = logState.value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CpuCategoryPagePreview() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "cpu") {
        composable("cpu") { CpuCategoryPage(navController) }
    }

    StabilityToolTheme {
        CpuCategoryPage(navController)
    }
} 