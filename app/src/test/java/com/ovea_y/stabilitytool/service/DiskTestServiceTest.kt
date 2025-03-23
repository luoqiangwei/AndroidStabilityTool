package com.ovea_y.stabilitytool.service

import android.content.Context
import android.os.Looper
import com.ovea_y.stabilitytool.TestUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class DiskTestServiceTest {

    private lateinit var mockContext: Context
    private lateinit var mockLogCallback: DiskTestService.LogCallback
    private lateinit var mockFile: File
    private lateinit var mockFileOutputStream: FileOutputStream
    
    private lateinit var diskTestService: DiskTestService
    
    @Before
    fun setUp() {
        // 使用mockito-kotlin创建mock对象
        mockContext = mock()
        mockLogCallback = mock()
        mockFile = mock()
        mockFileOutputStream = mock()
        
        // 模拟Context的getCacheDir方法
        whenever(mockContext.cacheDir).thenReturn(mockFile)
        
        // 模拟File的相关方法
        whenever(mockFile.exists()).thenReturn(true)
        whenever(mockFile.mkdirs()).thenReturn(true)
        whenever(mockFile.listFiles()).thenReturn(arrayOf())
        whenever(mockFile.delete()).thenReturn(true)
        
        // 创建DiskTestService实例
        diskTestService = DiskTestService()
        diskTestService.setLogCallback(mockLogCallback)
    }
    
    @Test
    fun testCauseSequentialWrite() {
        // 模拟文件创建
        val testFile = File.createTempFile("test", ".tmp")
        whenever(mockFile.createNewFile()).thenReturn(true)
        
        // 执行顺序写入测试
        diskTestService.causeSequentialWrite(mockContext, 1) // 使用较小的文件大小
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
        
        // 清理测试文件
        testFile.delete()
    }
    
    @Test
    fun testCauseSequentialRead() {
        // 模拟文件创建和写入
        val testFile = File.createTempFile("test", ".tmp")
        testFile.writeText("Test data")
        whenever(mockFile.exists()).thenReturn(true)
        whenever(mockFile.length()).thenReturn(testFile.length())
        
        // 执行顺序读取测试
        diskTestService.causeSequentialRead(mockContext, 1) // 使用较小的文件大小
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
        
        // 清理测试文件
        testFile.delete()
    }
    
    @Test
    fun testCauseRandomSmallFileIO() {
        // 执行随机小文件IO测试
        diskTestService.causeRandomSmallFileIO(mockContext, 10, 1) // 使用较小的文件数量和时间
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseFrequentFileCreateDelete() {
        // 执行频繁创建删除文件测试
        diskTestService.causeFrequentFileCreateDelete(mockContext, 1) // 使用较短的时间
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseFileFragmentation() {
        // 执行文件碎片化测试
        diskTestService.causeFileFragmentation(mockContext, 1) // 使用较短的时间
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseConcurrentIO() {
        // 执行并发IO测试
        diskTestService.causeConcurrentIO(mockContext, 1, 2) // 使用较短的时间和较少的线程
        
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
        diskTestService.causeFileFragmentation(mockContext, 10) // 使用较长的时间
        
        // 立即停止所有测试
        diskTestService.stopAllTests()
        
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