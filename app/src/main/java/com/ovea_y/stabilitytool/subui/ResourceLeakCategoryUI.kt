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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ovea_y.stabilitytool.TestStatus
import com.ovea_y.stabilitytool.rememberTestResultManager
import com.ovea_y.stabilitytool.service.ResourceLeakService
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.utils.findActivity
import android.provider.Settings
import android.net.Uri
import android.content.Intent
import android.os.Build
import android.app.Activity
import kotlinx.coroutines.launch

@Composable
fun ResourceLeakCategoryUI(
    navController: NavHostController,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val testResultManager = rememberTestResultManager()
    
    // 创建资源泄漏服务
    val resourceLeakService = remember { ResourceLeakService(context) }
    
    // 用于显示日志的状态
    val logState = remember { mutableStateOf("") }
    
    // 权限状态
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    // 检查权限状态的函数
    val checkOverlayPermission = {
        val hasPermission = Settings.canDrawOverlays(context)
        if (hasPermission != hasOverlayPermission) {
            hasOverlayPermission = hasPermission
            if (hasPermission) {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.overlay_permission_granted))
                }
            }
        }
    }
    
    // 请求悬浮窗权限
    val requestOverlayPermission: () -> Unit = {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            
            // 显示提示
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.please_allow_overlay))
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.cannot_open_settings, e.message))
            }
        }
    }
    
    // 当用户返回应用时检查权限状态
    LaunchedEffect(Unit) {
        val activity = try {
            context.findActivity()
        } catch (e: Exception) {
            null
        }
        
        activity?.let {
            it.window.decorView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    checkOverlayPermission()
                }
            }
        }
    }
    
    // 清除日志的函数
    val clearLog = {
        logState.value = ""
    }
    
    // 设置日志回调
    DisposableEffect(resourceLeakService) {
        resourceLeakService.setLogCallback(object : ResourceLeakService.LogCallback {
            override fun onLogUpdated(log: String) {
                logState.value += "$log\n"
            }
            
            override fun onTestStarted(testName: String) {
                // 开始新测试时重置测试状态
                testResultManager.resetTest()
                testResultManager.startTest(testName)
            }
            
            override fun onTestProgress(progress: Float, message: String) {
                testResultManager.updateProgress(progress, message)
            }
            
            override fun onTestCompleted(success: Boolean, message: String, details: String) {
                val status = if (success) TestStatus.SUCCESS else TestStatus.ERROR
                testResultManager.completeTest(status, message, details)
            }
        })
        
        onDispose {
            resourceLeakService.onDestroy()
            // 组件销毁时重置测试状态
            testResultManager.resetTest()
            clearLog()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = ColumnCommonPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部标题和返回按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = stringResource(id = R.string.resource_leak_test),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // 添加一个空的IconButton来平衡布局
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            // 提示信息
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = stringResource(id = R.string.resource_leak_common_tip),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 测试结果显示
            TestResultDisplay(
                title = testResultManager.testResult.value.title,
                status = testResultManager.testResult.value.status,
                progress = testResultManager.testResult.value.progress,
                message = testResultManager.testResult.value.message,
                details = testResultManager.testResult.value.details,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 权限检查按钮
            if (!hasOverlayPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.missing_overlay_permission),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = stringResource(R.string.overlay_permission_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Button(
                            onClick = { requestOverlayPermission() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.authorize_now))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 日志显示区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.test_logs),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Button(
                        onClick = clearLog,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.clear_log))
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val scrollState = rememberScrollState()
                    
                    LaunchedEffect(logState.value) {
                        // 自动滚动到最新内容
                        if (logState.value.isNotEmpty()) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                    
                    Text(
                        text = logState.value.ifEmpty { stringResource(R.string.no_logs) },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            }
            
            // 窗口泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_window),
                onClick = { 
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    
                    // 检查悬浮窗权限
                    if (!hasOverlayPermission) {
                        // 显示权限请求提示
                        testResultManager.startTest(context.getString(R.string.window_leak_test))
                        testResultManager.updateMessage(context.getString(R.string.permission_required))
                        
                        // 请求权限
                        requestOverlayPermission()
                    } else {
                        // 有权限，开始测试
                        resourceLeakService.causeWindowLeak() 
                    }
                }
            )
            
            // Activity泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_activity),
                onClick = {
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    try {
                        val activity = context.findActivity()
                        resourceLeakService.causeActivityLeak(activity)
                    } catch (e: Exception) {
                        // 使用testResultManager代替直接调用resourceLeakService的方法
                        testResultManager.startTest(context.getString(R.string.activity_leak_test))
                        testResultManager.completeTest(TestStatus.ERROR, context.getString(R.string.activity_instance_error), e.message ?: context.getString(R.string.unknown_error))
                    }
                }
            )
            
            // PendingIntent泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_pending_intent),
                onClick = { 
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.causePendingIntentLeak() 
                }
            )
            
            // 闹钟泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_alarm),
                onClick = { 
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.causeAlarmLeak() 
                }
            )
            
            // Binder代理泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_binder_proxy),
                onClick = { 
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.causeBinderProxyLeak() 
                }
            )
            
            // 任务调度器泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_job),
                onClick = { 
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.causeJobSchedulerLeak() 
                }
            )
            
            // Handler泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_handler),
                onClick = {
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.causeHandlerLeak()
                }
            )
            
            // 线程泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_thread),
                onClick = {
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.causeThreadLeak()
                }
            )
            
            // Context泄漏测试
            CommonButton(
                text = stringResource(id = R.string.resource_leak_context),
                onClick = { 
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.causeContextLeak() 
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 清理资源泄漏
            CommonButton(
                text = stringResource(id = R.string.resource_leak_clear),
                onClick = {
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.clearResourceLeaks()
                }
            )
            
            // 停止所有测试
            CommonButton(
                text = stringResource(id = R.string.stop_all_tests),
                onClick = { 
                    // 开始新测试前重置测试状态
                    testResultManager.resetTest()
                    resourceLeakService.stopAllTests() 
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview
@Composable
fun ResourceLeakCategoryUIPreview() {
    ResourceLeakCategoryUI(
        navController = rememberNavController(),
        onBackPressed = {}
    )
} 