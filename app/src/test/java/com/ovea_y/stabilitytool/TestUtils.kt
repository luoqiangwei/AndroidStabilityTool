package com.ovea_y.stabilitytool

import android.os.Looper
import org.robolectric.Shadows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 测试工具类
 * 
 * 提供通用的测试辅助方法
 */
object TestUtils {
    
    /**
     * 等待主线程上的所有消息被处理
     */
    fun waitForMainThreadTasks() {
        // 使用Robolectric的ShadowLooper确保主线程上的所有消息都被处理
        try {
            // 处理所有待处理的消息
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            
            // 额外的短暂延迟，确保异步操作有时间启动
            Thread.sleep(100)
            
            // 再次处理所有消息
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        } catch (e: Exception) {
            // 如果不在Robolectric环境中，则使用简单的延迟
            Thread.sleep(100)
        }
    }
    
    /**
     * 等待指定的时间
     * 
     * @param millis 等待的毫秒数
     */
    fun waitFor(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    
    /**
     * 等待异步操作完成
     * 
     * @param latch 倒计时锁
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否在超时前完成
     */
    fun await(latch: CountDownLatch, timeout: Long = 5, unit: TimeUnit = TimeUnit.SECONDS): Boolean {
        return try {
            latch.await(timeout, unit)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 在主线程上执行操作
     * 
     * @param action 要执行的操作
     */
    fun runOnMainThread(action: () -> Unit) {
        val latch = CountDownLatch(1)
        Looper.getMainLooper().thread.run {
            action()
            latch.countDown()
        }
        await(latch)
    }
    
    /**
     * 创建指定大小的测试数据
     * 
     * @param sizeInBytes 数据大小（字节）
     * @return 测试数据
     */
    fun createTestData(sizeInBytes: Int): ByteArray {
        val data = ByteArray(sizeInBytes)
        for (i in data.indices) {
            data[i] = (i % 256).toByte()
        }
        return data
    }
} 