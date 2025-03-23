package com.ovea_y.stabilitytool.service

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.ovea_y.stabilitytool.R
import com.ovea_y.stabilitytool.TestUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.Mockito.mockStatic
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
class ResourceLeakServiceTest {

    private lateinit var mockContext: Context
    private lateinit var mockWindowManager: WindowManager
    private lateinit var mockJobScheduler: JobScheduler
    private lateinit var mockAlarmManager: AlarmManager
    private lateinit var mockLogCallback: ResourceLeakService.LogCallback
    
    private lateinit var resourceLeakService: ResourceLeakService
    
    @Before
    fun setUp() {
        // 使用mockito-kotlin创建mock对象
        mockContext = mock()
        mockWindowManager = mock()
        mockJobScheduler = mock()
        mockAlarmManager = mock()
        mockLogCallback = mock()
        
        // 模拟Context的getString方法
        whenever(mockContext.getString(any())).thenReturn("测试字符串")
        whenever(mockContext.getString(any(), any())).thenReturn("测试字符串带参数")
        
        // 模拟Context的getSystemService方法
        whenever(mockContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mockWindowManager)
        whenever(mockContext.getSystemService(Context.JOB_SCHEDULER_SERVICE)).thenReturn(mockJobScheduler)
        whenever(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)
        
        // 模拟Settings.canDrawOverlays方法
        val settingsMock = mockStatic(Settings::class.java)
        settingsMock.`when`<Boolean> { Settings.canDrawOverlays(mockContext) }.thenReturn(true)
        
        // 创建ResourceLeakService实例
        resourceLeakService = ResourceLeakService(mockContext)
        resourceLeakService.setLogCallback(mockLogCallback)
    }
    
    @Test
    fun testCauseWindowLeak() {
        // 模拟WindowManager.addView方法
        doAnswer { invocation ->
            val view = invocation.getArgument<View>(0)
            // 不执行实际操作
            null
        }.whenever(mockWindowManager).addView(any(), any())
        
        // 执行窗口泄漏测试
        resourceLeakService.causeWindowLeak(count = 2)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCausePendingIntentLeak() {
        // 执行PendingIntent泄漏测试
        resourceLeakService.causePendingIntentLeak(count = 2)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestProgress(any(), any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testCauseActivityLeak() {
        // 模拟Activity
        val mockActivity = mock<Activity>()
        
        // 执行Activity泄漏测试
        resourceLeakService.causeActivityLeak(mockActivity)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 验证回调方法被调用
        verify(mockLogCallback).onTestStarted(any())
        verify(mockLogCallback).onTestCompleted(any(), any(), any())
    }
    
    @Test
    fun testClearResourceLeaks() {
        // 先执行一些泄漏测试
        resourceLeakService.causeWindowLeak(count = 1)
        resourceLeakService.causePendingIntentLeak(count = 1)
        
        // 等待测试完成
        waitForTestCompletion()
        
        // 执行清理操作
        resourceLeakService.clearResourceLeaks()
        
        // 等待清理完成
        waitForTestCompletion()
        
        // 验证清理回调被调用
        verify(mockLogCallback).onTestStarted(any())
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