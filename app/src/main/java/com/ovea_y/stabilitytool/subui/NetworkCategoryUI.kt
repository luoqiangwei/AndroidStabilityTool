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
import com.ovea_y.stabilitytool.service.NetWorkTestService
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme
import kotlinx.coroutines.launch

@Composable
fun NetworkCategoryPage(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val networkTestService = NetWorkTestService()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val networkCommonTip = stringResource(R.string.network_common_tip)
    val currentNetworkStatusPrefix = stringResource(R.string.current_network_status)
    val stoppingTestsMessage = stringResource(R.string.stopping_tests)
    val context = LocalContext.current
    
    // 获取当前网络状态
    val networkStatus = networkTestService.checkNetworkStatus(context)

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
                    stringResource(id = R.string.network_test),
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
                
                // 显示当前网络状态
                Text(
                    text = "${stringResource(R.string.current_network_status)}: $networkStatus",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Row() {
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize(0.5f)) {

                        // 大文件下载测试
                        CommonButton(text = stringResource(R.string.network_large_file_download)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(networkCommonTip)
                            }
                            networkTestService.causeLargeFileDownload(context)
                        }

                        // 并发连接测试
                        CommonButton(text = stringResource(R.string.network_concurrent_connections)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(networkCommonTip)
                            }
                            networkTestService.causeConcurrentConnections(50, 60)
                        }

                        // 网络延迟测试
                        CommonButton(text = stringResource(R.string.network_latency_test)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(networkCommonTip)
                            }
                            networkTestService.causeNetworkLatencyTest("8.8.8.8", 50)
                        }

                        // 网络抖动测试
                        CommonButton(text = stringResource(R.string.network_jitter_test)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(networkCommonTip)
                            }
                            networkTestService.causeNetworkJitterTest("8.8.8.8", 60)
                        }
                    }
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize()) {

                        // 网络带宽占用测试
                        CommonButton(text = stringResource(R.string.network_bandwidth_consumption)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(networkCommonTip)
                            }
                            networkTestService.causeBandwidthConsumption(context, 60, 3)
                        }

                        // 网络连接泄漏测试
                        CommonButton(text = stringResource(R.string.network_connection_leak)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(networkCommonTip)
                            }
                            networkTestService.causeConnectionLeak(60, 50)
                        }

                        // DNS解析压力测试
                        CommonButton(text = stringResource(R.string.network_dns_resolution_stress)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(networkCommonTip)
                            }
                            networkTestService.causeDnsResolutionStress(60)
                        }

                        // 刷新网络状态
                        CommonButton(text = stringResource(R.string.network_refresh_status)) {
                            coroutineScope.launch {
                                val status = networkTestService.checkNetworkStatus(context)
                                snackbarHostState.showSnackbar("$currentNetworkStatusPrefix: $status")
                            }
                        }

                        // 停止所有测试
                        CommonButton(text = stringResource(R.string.stop_all_tests)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(stoppingTestsMessage)
                            }
                            networkTestService.stopAllTests()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkCategoryPagePreview() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "network") {
        composable("network") { NetworkCategoryPage(navController) }
    }

    StabilityToolTheme {
        NetworkCategoryPage(navController)
    }
} 