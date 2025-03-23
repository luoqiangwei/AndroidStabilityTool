package com.ovea_y.stabilitytool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 测试结果数据类
 */
data class TestResult(
    val title: String,
    val status: TestStatus = TestStatus.IDLE,
    val progress: Float = 0f,
    val message: String = "",
    val details: String = ""
)

/**
 * 测试结果管理器
 */
class TestResultManager(private val coroutineScope: CoroutineScope) {
    // 测试结果状态
    val testResult = mutableStateOf(TestResult(""))
    
    // 当前运行的任务
    private var currentJob: Job? = null
    
    /**
     * 开始测试
     */
    fun startTest(title: String, initialMessage: String = "测试开始...") {
        testResult.value = TestResult(
            title = title,
            status = TestStatus.RUNNING,
            progress = 0f,
            message = initialMessage
        )
    }
    
    /**
     * 更新测试进度
     */
    fun updateProgress(progress: Float, message: String = "") {
        testResult.value = testResult.value.copy(
            progress = progress,
            message = message.ifEmpty { testResult.value.message }
        )
    }
    
    /**
     * 更新测试消息
     */
    fun updateMessage(message: String) {
        testResult.value = testResult.value.copy(message = message)
    }
    
    /**
     * 更新测试详细信息
     */
    fun updateDetails(details: String) {
        testResult.value = testResult.value.copy(details = details)
    }
    
    /**
     * 完成测试
     */
    fun completeTest(status: TestStatus, message: String, details: String = "") {
        testResult.value = testResult.value.copy(
            status = status,
            progress = 1f,
            message = message,
            details = details
        )
        
        // 如果是成功或警告状态，5秒后自动清除
        if (status == TestStatus.SUCCESS || status == TestStatus.WARNING) {
            currentJob?.cancel()
            currentJob = coroutineScope.launch {
                delay(5000)
                resetTest()
            }
        }
    }
    
    /**
     * 重置测试
     */
    fun resetTest() {
        testResult.value = TestResult(title = "")
        currentJob?.cancel()
        currentJob = null
    }
    
    /**
     * 测试成功
     */
    fun testSuccess(message: String, details: String = "") {
        completeTest(TestStatus.SUCCESS, message, details)
    }
    
    /**
     * 测试警告
     */
    fun testWarning(message: String, details: String = "") {
        completeTest(TestStatus.WARNING, message, details)
    }
    
    /**
     * 测试错误
     */
    fun testError(message: String, details: String = "") {
        completeTest(TestStatus.ERROR, message, details)
    }
}

/**
 * 创建测试结果管理器
 */
@Composable
fun rememberTestResultManager(): TestResultManager {
    val coroutineScope = rememberCoroutineScope()
    return remember { TestResultManager(coroutineScope) }
} 