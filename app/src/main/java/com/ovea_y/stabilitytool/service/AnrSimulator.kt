package com.ovea_y.stabilitytool.service

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * ANR类型
 * - 1. Input超时 （5s），最主要的ANR，大部分都是主线程堵塞引起，少部分是系统自身问题
 *      - 1.1 长时间I/O
 *          - 1.1.1 网络
 *          - 1.1.2 文件
 *      - 1.2 死循环或耗时计算
 *      - 1.3 sleep或wait操作
 *      - 1.4 死锁
 *      - 1.5 长时间UI操作
 * - 2. Broadcast Receiver超时 （10s）
 * - 3. ContentProvider超时 （10s）
 * - 4. Service超时 （前台20s，后台200s）
 */

class AnrSimulator {

    // 1. 使用 Thread.sleep() 制造主线程休眠
    fun causeSleepANR() {
        Thread.sleep(20000)  // 主线程休眠 20 秒
    }

    fun causeIOANR(context: Context) {
        val file = File(context.filesDir, "large_file.dat")
        file.delete()

        val sizeInBytes = 1000 * 1024 * 1024 // 1000MB
        val chunkSize = 10 * 1024 * 1024 // 每次写入10MB，避免一次性分配太多内存

        // 分批写入数据
        FileOutputStream(file).use { output ->
            repeat(sizeInBytes / chunkSize) {
                val randomData = ByteArray(chunkSize)
                Random.nextBytes(randomData)  // 生成随机数据
                output.write(randomData)
            }
        }

        // 在主线程中分批读取大文件，制造ANR
        if (file.exists()) {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(chunkSize)
                while (input.read(buffer) != -1) {
                    // 模拟处理读取的数据，阻塞主线程
                }
            }
        }
    }

    // 3. 死循环，模拟复杂计算
    fun causeLoopANR() {
        while (true) {
            // 无限循环，阻塞主线程
        }
    }

    // 4. 死锁，主线程等待资源争夺
    fun causeDeadlockANR() {
        val lock1 = Object()
        val lock2 = Object()

        Thread {
            synchronized(lock1) {
                Thread.sleep(100)
                synchronized(lock2) {

                }
            }
        }.start()

        synchronized(lock2) {
            Thread.sleep(100)
            synchronized(lock1) {

            }
        }
    }

    // 5. 在主线程中频繁更新 UI
    @Composable
    fun causeUIANR(itemCount: Int) {
        // 当 itemCount 大于 0 时，生成大量 UI 元素
        if (itemCount > 0) {
            Column {
                for (i in 0..itemCount) {
                    Text(text = "This is item $i")
                }
            }
        }
    }

    fun causeNetworkIOANR() {
        val url = URL("https://github.com/torvalds/linux/archive/refs/tags/v6.11.zip")  // 假设这是一个大文件的URL
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 0  // 无连接超时
            connection.readTimeout = 0     // 无读取超时
            connection.requestMethod = "GET"

            // 在主线程中执行网络请求，模拟长时间阻塞
            val inputStream = connection.inputStream
            val data = inputStream.readBytes()  // 读取网络数据，阻塞主线程
            inputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }
}
