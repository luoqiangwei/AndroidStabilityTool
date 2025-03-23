package com.ovea_y.stabilitytool.service

import android.os.Looper
import com.ovea_y.stabilitytool.TestUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class MemoryTestServiceTest {

    private lateinit var mockLogCallback: MemoryTestService.LogCallback
    private lateinit var memoryTestService: MemoryTestService
    
    @Before
    fun setUp() {
        // 使用mockito-kotlin创建mock对象
        mockLogCallback = mock()
        
        // 创建MemoryTestService实例
        memoryTestService = MemoryTestService()
        memoryTestService.setLogCallback(mockLogCallback)
    }
    
    @Test
    fun testCauseMemoryLeak() {
        // 使用较小的值进行测试，避免OOM
        memoryTestService.causeMemoryLeak(leakSizeMB = 1, leakStepMB = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseMemoryChurn() {
        // 使用较小的时间进行测试
        memoryTestService.causeMemoryChurn(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseLargeObjectAllocation() {
        // 使用较小的值进行测试，避免OOM
        memoryTestService.causeLargeObjectAllocation(sizeMB = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseMemoryFragmentation() {
        // 使用较小的时间进行测试
        memoryTestService.causeMemoryFragmentation(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testClearMemoryLeak() {
        // 先执行内存泄漏测试
        memoryTestService.causeMemoryLeak(leakSizeMB = 1, leakStepMB = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 执行清理操作
        memoryTestService.clearMemoryLeak()
        
        // 等待清理完成
        waitForTestCompletion()
        
        // 验证清理回调被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testStopAllTests() {
        // 启动一个长时间运行的测试
        memoryTestService.causeMemoryChurn(durationSeconds = 10)
        
        // 立即停止所有测试
        memoryTestService.stopAllTests()
        
        // 等待操作完成
        waitForTestCompletion()
        
        // 验证停止回调被调用
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    private fun waitForTestCompletion() {
        // 使用Robolectric的ShadowLooper确保主线程上的所有消息都被处理
        val shadowLooper = Shadows.shadowOf(Looper.getMainLooper())
        shadowLooper.idle() // 处理所有待处理的消息
        
        // 等待主线程上的所有消息被处理
        TestUtils.waitForMainThreadTasks()
        
        // 再次确保所有消息都被处理
        shadowLooper.idle()
        
        // 额外等待一些时间，确保异步操作完成
        TestUtils.waitFor(1000) // 增加等待时间到1000毫秒
        
        // 最后再次确保所有消息都被处理
        shadowLooper.idle()
    }
} 