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
import com.ovea_y.stabilitytool.TestResult
import com.ovea_y.stabilitytool.TestResultDisplay
import com.ovea_y.stabilitytool.TestStatus
import com.ovea_y.stabilitytool.rememberTestResultManager
import com.ovea_y.stabilitytool.service.DiskTestService
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme
import kotlinx.coroutines.launch

@Composable
fun DiskCategoryPage(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val diskTestService = remember { DiskTestService() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val diskCommonTip = stringResource(R.string.disk_common_tip)
    val stoppingTestsMessage = stringResource(R.string.stopping_tests)
    val context = LocalContext.current
    
    // 测试结果管理器
    val testResultManager = rememberTestResultManager()
    
    // 日志状态
    val logState = remember { mutableStateOf("") }
    
    // 设置日志回调
    DisposableEffect(diskTestService) {
        diskTestService.setLogCallback(object : DiskTestService.LogCallback {
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
            diskTestService.onDestroy()
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
                    stringResource(id = R.string.disk_test),
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

                        // 顺序写入大文件测试
                        CommonButton(text = stringResource(R.string.disk_sequential_write)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(diskCommonTip)
                            }
                            diskTestService.causeSequentialWrite(context, 100) // 使用较小的文件大小以加快测试
                        }

                        // 顺序读取大文件测试
                        CommonButton(text = stringResource(R.string.disk_sequential_read)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(diskCommonTip)
                            }
                            diskTestService.causeSequentialRead(context, 100) // 使用较小的文件大小以加快测试
                        }

                        // 随机读写小文件测试
                        CommonButton(text = stringResource(R.string.disk_random_small_file)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(diskCommonTip)
                            }
                            diskTestService.causeRandomSmallFileIO(context, 500, 60)
                        }

                        // 频繁创建删除文件测试
                        CommonButton(text = stringResource(R.string.disk_frequent_create_delete)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(diskCommonTip)
                            }
                            diskTestService.causeFrequentFileCreateDelete(context, 60)
                        }
                    }
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize()) {

                        // 文件碎片化测试
                        CommonButton(text = stringResource(R.string.disk_fragmentation)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(diskCommonTip)
                            }
                            diskTestService.causeFileFragmentation(context, 60)
                        }

                        // 并发读写测试
                        CommonButton(text = stringResource(R.string.disk_concurrent_io)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(diskCommonTip)
                            }
                            diskTestService.causeConcurrentIO(context, 60, 4)
                        }

                        // 文件系统填满测试
                        CommonButton(text = stringResource(R.string.disk_filesystem_fill)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(diskCommonTip)
                            }
                            diskTestService.causeFilesystemFill(context, 80) // 使用80%作为目标，避免完全填满
                        }

                        // 停止所有测试
                        CommonButton(text = stringResource(R.string.stop_all_tests)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(stoppingTestsMessage)
                            }
                            diskTestService.stopAllTests()
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
fun DiskCategoryPagePreview() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "disk") {
        composable("disk") { DiskCategoryPage(navController) }
    }

    StabilityToolTheme {
        DiskCategoryPage(navController)
    }
} 