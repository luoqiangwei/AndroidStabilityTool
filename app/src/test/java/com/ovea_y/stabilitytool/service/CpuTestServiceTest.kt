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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class CpuTestServiceTest {

    private lateinit var mockLogCallback: CpuTestService.LogCallback
    private lateinit var cpuTestService: CpuTestService
    
    @Before
    fun setUp() {
        // 使用mockito-kotlin创建mock对象
        mockLogCallback = mock()
        
        // 创建CpuTestService实例
        cpuTestService = CpuTestService()
        cpuTestService.setLogCallback(mockLogCallback)
    }
    
    @Test
    fun testCauseSingleCoreHighLoad() {
        // 执行单核高负载测试，使用较短的时间
        cpuTestService.causeSingleCoreHighLoad(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseMultiCoreHighLoad() {
        // 执行多核高负载测试，使用较短的时间
        cpuTestService.causeMultiCoreHighLoad(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseFrequentGC() {
        // 执行频繁GC测试，使用较短的时间
        cpuTestService.causeFrequentGC(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseFrequentThreadCreation() {
        // 执行频繁线程创建测试，使用较短的时间
        cpuTestService.causeFrequentThreadCreation(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCausePrimeNumberCalculation() {
        // 执行素数计算测试，使用较短的时间
        cpuTestService.causePrimeNumberCalculation(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseTemperatureRise() {
        // 执行温度升高测试，使用较短的时间
        cpuTestService.causeTemperatureRise(durationSeconds = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testStopAllTests() {
        // 启动一个长时间运行的测试
        cpuTestService.causeSingleCoreHighLoad(durationSeconds = 10)
        
        // 立即停止所有测试
        cpuTestService.stopAllTests()
        
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