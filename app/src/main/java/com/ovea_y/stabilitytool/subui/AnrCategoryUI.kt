package com.ovea_y.stabilitytool.subui

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Looper
import android.util.Log
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.ovea_y.stabilitytool.service.AnrSimulator
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme
import kotlinx.coroutines.launch

fun getLatestProcessExitReason(context: Context): ApplicationExitInfo? {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 1)

    // 检查是否有退出原因
    return if (exitReasons.isNotEmpty() && exitReasons[0].reason == ApplicationExitInfo.REASON_ANR) {
        exitReasons[0]  // 返回最新的退出原因
    } else {
        null  // 没有退出原因时返回 null
    }
}

@Composable
fun AnrCategoryPage(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val anrSimulator = AnrSimulator()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val anrCommonTip = stringResource(R.string.anr_common_tip)
    val anrInputTip = stringResource(R.string.anr_input_tip)
    val context = LocalContext.current
    val latestExitReason = getLatestProcessExitReason(context)
    var itemCount by remember { mutableStateOf(0) }

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
                    stringResource(id = R.string.anr_test),
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

                if (latestExitReason != null) {
                    Text("Last ANR Reason: ${latestExitReason.description}")
                }
                
                // Input ANR 测试部分
                Text(
                    "Input ANR 测试 (5秒超时)",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start
                )
                
                Row() {
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize(0.5f)) {

                        CommonButton(text = stringResource(R.string.anr_input_sleep)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeSleepANR()
                            }, 1000)
                        }

                        CommonButton(text = stringResource(R.string.anr_input_loop_or_timeout)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeLoopANR()
                            }, 1000)
                        }

                        CommonButton(text = stringResource(R.string.anr_input_io_file)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeIOANR(context)
                            }, 1000)
                        }
                        
                        CommonButton(text = "线程等待ANR") {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeThreadWaitANR()
                            }, 1000)
                        }

//                        CommonButton(text = "Input无焦点窗口ANR") {
//                            coroutineScope.launch {
//                                snackbarHostState.showSnackbar("重新启动程序，且不断点击屏幕触发")
//                            }
//                            android.os.Handler(Looper.getMainLooper()).postDelayed({
//                                anrSimulator.causeNotFocusedWindowANR(context)
//                            }, 1000)
//                        }
                    }
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize()) {

                        CommonButton(text = stringResource(R.string.anr_input_deadlock)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeDeadlockANR()
                            }, 1000)
                        }

                        CommonButton(text = stringResource(R.string.anr_input_ui_timeout)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                itemCount =  if (itemCount == 0) 100000 else 0  // 设置 itemCount 为一个很大的值来阻塞UI
                            }, 1000)
                        }
                        // Test UI ANR
                        anrSimulator.causeUIANR(itemCount)
                        
                        CommonButton(text = "同步方法ANR") {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeSynchronizedMethodANR()
                            }, 1000)
                        }
                        
                        CommonButton(text = "系统调用ANR") {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(anrInputTip)
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeSystemCallANR(context)
                            }, 1000)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // 组件ANR测试部分
                Text(
                    "组件ANR测试",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start
                )
                
                Row() {
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize(0.5f)) {
                        
                        CommonButton(text = "广播接收器ANR (10秒)") {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("将触发广播接收器ANR，请等待...")
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeBroadcastReceiverANR(context)
                            }, 1000)
                        }
                        
                        CommonButton(text = "Service ANR (20秒)") {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("将触发Service ANR，请等待...")
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeServiceANR(context)
                            }, 1000)
                        }
                    }
                    
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize()) {
                        
                        // ContentProvider需要在AndroidManifest.xml中注册，这里只是显示按钮
                        CommonButton(text = "App启动超时未完成") {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("将触发App启动 ANR，请重新点击App触发异常")
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                anrSimulator.causeAppStartANR(context)
                            }, 1000)
                        }
                        
                        // StrictMode: NetworkOnMainThreadException
                        CommonButton(text = "网络IO ANR") {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("注意：在Android 9+上，这会触发StrictMode异常而不是ANR")
                            }
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    anrSimulator.causeNetworkIOANR()
                                } catch (e: Exception) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("触发了异常: ${e.javaClass.simpleName}")
                                    }
                                }
                            }, 1000)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnrCategoryPagePreview() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "anr") {
        composable("anr") { AnrCategoryPage(navController) }
    }

    StabilityToolTheme {
        AnrCategoryPage(navController)
    }
}