package com.ovea_y.stabilitytool.subui

import android.content.Context
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
import com.ovea_y.stabilitytool.service.CrashService
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme
import kotlinx.coroutines.launch

@Composable
fun CrashCategoryPage(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val crashService = CrashService()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val crashCommonTip = stringResource(R.string.crash_common_tip)
    val context = LocalContext.current

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
                    stringResource(id = R.string.crash_test),
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

                        // Java崩溃类别
                        CommonButton(text = stringResource(R.string.crash_null_pointer)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeNullPointerException()
                            }, 1000)
                        }

                        CommonButton(text = stringResource(R.string.crash_array_index)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeArrayIndexOutOfBoundsException()
                            }, 1000)
                        }

                        CommonButton(text = stringResource(R.string.crash_class_cast)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeClassCastException()
                            }, 1000)
                        }

                        CommonButton(text = stringResource(R.string.crash_oom)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeOutOfMemoryError()
                            }, 1000)
                        }
                    }
                    Column(modifier = Modifier
                        .padding(all = ColumnCommonPadding)
                        .fillMaxSize()) {

                        CommonButton(text = stringResource(R.string.crash_stack_overflow)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeStackOverflowError()
                            }, 1000)
                        }

                        // Native崩溃类别
                        CommonButton(text = stringResource(R.string.crash_sigsegv)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeSIGSEGV()
                            }, 1000)
                        }

                        CommonButton(text = stringResource(R.string.crash_sigabrt)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeSIGABRT()
                            }, 1000)
                        }

                        // 应用主动退出
                        CommonButton(text = stringResource(R.string.crash_app_exit)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeAppExit()
                            }, 1000)
                        }

                        // 文件系统崩溃
                        CommonButton(text = stringResource(R.string.crash_filesystem)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(crashCommonTip)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                crashService.causeFileSystemCrash(context)
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
fun CrashCategoryPagePreview() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "crash") {
        composable("crash") { CrashCategoryPage(navController) }
    }

    StabilityToolTheme {
        CrashCategoryPage(navController)
    }
} 