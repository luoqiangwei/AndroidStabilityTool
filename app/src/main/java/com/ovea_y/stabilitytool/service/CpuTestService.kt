package com.ovea_y.stabilitytool.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * CPU性能测试类型
 * - 1. 单核高负载
 * - 2. 多核高负载
 * - 3. 频繁GC
 * - 4. 高频线程创建与销毁
 * - 5. 复杂计算（素数计算）
 * - 6. 温度升高测试
 */
class CpuTestService {
    private val TAG = "CpuTestService"
    private val executor = Executors.newCachedThreadPool()
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 日志回调接口
    interface LogCallback {
        fun onLogUpdated(log: String)
        fun onTestStarted(testName: String)
        fun onTestProgress(progress: Float, message: String)
        fun onTestCompleted(success: Boolean, message: String, details: String = "")
    }
    
    private var logCallback: LogCallback? = null
    
    // 设置日志回调
    fun setLogCallback(callback: LogCallback) {
        this.logCallback = callback
    }
    
    // 记录日志并回调
    private fun logInfo(message: String) {
        Log.d(TAG, message)
        mainHandler.post {
            logCallback?.onLogUpdated(message)
        }
    }

    // 1. 单核高负载测试
    fun causeSingleCoreHighLoad(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始单核高负载测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("单核高负载测试")
        
        thread {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationSeconds * 1000
            
            // 单线程执行密集计算
            while (System.currentTimeMillis() < endTime && isRunning) {
                // 执行一些无意义但CPU密集的计算
                var result = 0.0
                for (i in 0 until 10000000) {
                    result += sqrt(i.toDouble()) * Math.sin(i.toDouble())
                }
                
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                val totalTime = durationSeconds * 1000
                val progress = elapsedTime.toFloat() / totalTime
                
                if (currentTime % 1000 < 100) { // 大约每秒更新一次
                    val progressMessage = "单核高负载测试进行中，已完成: ${(progress * 100).toInt()}%"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                }
            }
            
            isRunning = false
            val resultMessage = "单核高负载测试结束"
            logInfo(resultMessage)
            mainHandler.post {
                logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
            }
        }
    }

    // 2. 多核高负载测试
    fun causeMultiCoreHighLoad(durationSeconds: Int = 60, threadCount: Int = Runtime.getRuntime().availableProcessors()) {
        if (isRunning) return
        isRunning = true
        
        val startMessage = "开始多核高负载测试，线程数: $threadCount，持续${durationSeconds}秒"
        logInfo(startMessage)
        logCallback?.onTestStarted("多核高负载测试")
        
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationSeconds * 1000
        
        // 创建多个线程执行密集计算
        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    while (System.currentTimeMillis() < endTime && isRunning) {
                        // 执行一些无意义但CPU密集的计算
                        var result = 0.0
                        for (i in 0 until 5000000) {
                            result += sqrt(i.toDouble()) * Math.sin(i.toDouble())
                        }
                        
                        // 每个线程独立计算，但只在主线程更新进度
                        if (threadId == 0) {
                            val currentTime = System.currentTimeMillis()
                            val elapsedTime = currentTime - startTime
                            val totalTime = durationSeconds * 1000
                            val progress = elapsedTime.toFloat() / totalTime
                            
                            if (currentTime % 1000 < 100) { // 大约每秒更新一次
                                val progressMessage = "多核高负载测试进行中，已完成: ${(progress * 100).toInt()}%"
                                logInfo(progressMessage)
                                mainHandler.post {
                                    logCallback?.onTestProgress(progress, progressMessage)
                                }
                            }
                        }
                    }
                    
                    if (threadId == 0 && !isRunning) {
                        val resultMessage = "多核高负载测试结束"
                        logInfo(resultMessage)
                        mainHandler.post {
                            logCallback?.onTestCompleted(true, resultMessage, "线程数: $threadCount, 持续时间: ${durationSeconds}秒")
                        }
                    }
                } catch (e: Exception) {
                    if (threadId == 0) {
                        val errorMessage = "多核高负载测试异常: ${e.message}"
                        logInfo(errorMessage)
                        mainHandler.post {
                            logCallback?.onTestCompleted(false, errorMessage)
                        }
                    }
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            if (isRunning) {
                isRunning = false
                val resultMessage = "多核高负载测试结束"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "线程数: $threadCount, 持续时间: ${durationSeconds}秒")
                }
            }
        }, durationSeconds * 1000L)
    }
    
    // 3. 频繁GC测试
    fun causeFrequentGC(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始频繁GC测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("频繁GC测试")
        
        thread {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationSeconds * 1000
            var gcCount = 0
            
            while (System.currentTimeMillis() < endTime && isRunning) {
                // 创建大量临时对象触发GC
                val list = ArrayList<ByteArray>(100)
                for (i in 0 until 100) {
                    list.add(ByteArray(1024 * 1024)) // 1MB
                }
                list.clear()
                
                // 主动请求GC
                System.gc()
                gcCount++
                
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                val totalTime = durationSeconds * 1000
                val progress = elapsedTime.toFloat() / totalTime
                
                if (currentTime % 1000 < 100) { // 大约每秒更新一次
                    val progressMessage = "频繁GC测试进行中，已触发GC: $gcCount 次"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                }
                
                try {
                    Thread.sleep(100) // 短暂休眠，避免过于频繁
                } catch (e: InterruptedException) {
                    break
                }
            }
            
            isRunning = false
            val resultMessage = "频繁GC测试结束，总共触发GC: $gcCount 次"
            logInfo(resultMessage)
            mainHandler.post {
                logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
            }
        }
    }
    
    // 4. 高频线程创建与销毁测试
    fun causeFrequentThreadCreation(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始高频线程创建与销毁测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("高频线程创建与销毁测试")
        
        thread {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationSeconds * 1000
            var threadCount = 0
            
            while (System.currentTimeMillis() < endTime && isRunning) {
                // 创建并启动线程
                val threads = ArrayList<Thread>()
                for (i in 0 until 10) {
                    val t = Thread {
                        // 线程内执行一些简单计算
                        var result = 0.0
                        for (j in 0 until 1000000) {
                            result += j
                        }
                    }
                    t.start()
                    threads.add(t)
                    threadCount++
                }
                
                // 等待所有线程结束
                for (t in threads) {
                    try {
                        t.join()
                    } catch (e: InterruptedException) {
                        break
                    }
                }
                
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                val totalTime = durationSeconds * 1000
                val progress = elapsedTime.toFloat() / totalTime
                
                if (currentTime % 1000 < 100) { // 大约每秒更新一次
                    val progressMessage = "高频线程创建测试进行中，已创建线程: $threadCount 个"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                }
            }
            
            isRunning = false
            val resultMessage = "高频线程创建与销毁测试结束，总共创建线程: $threadCount 个"
            logInfo(resultMessage)
            mainHandler.post {
                logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
            }
        }
    }
    
    // 5. 复杂计算（素数计算）测试
    fun causePrimeNumberCalculation(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始素数计算测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("素数计算测试")
        
        thread {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationSeconds * 1000
            var primeCount = 0
            var currentNumber = 2L
            
            while (System.currentTimeMillis() < endTime && isRunning) {
                if (isPrime(currentNumber)) {
                    primeCount++
                }
                currentNumber++
                
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                val totalTime = durationSeconds * 1000
                val progress = elapsedTime.toFloat() / totalTime
                
                if (currentTime % 1000 < 100) { // 大约每秒更新一次
                    val progressMessage = "素数计算测试进行中，已找到素数: $primeCount 个，当前检查: $currentNumber"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                }
            }
            
            isRunning = false
            val resultMessage = "素数计算测试结束，总共找到素数: $primeCount 个，最大检查到: $currentNumber"
            logInfo(resultMessage)
            mainHandler.post {
                logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
            }
        }
    }
    
    // 判断一个数是否为素数
    private fun isPrime(n: Long): Boolean {
        if (n <= 1) return false
        if (n <= 3) return true
        if (n % 2 == 0L || n % 3 == 0L) return false
        
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2) == 0L) return false
            i += 6
        }
        return true
    }
    
    // 6. 温度升高测试（长时间全核心高负载）
    fun causeTemperatureRise(durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始温度升高测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("温度升高测试")
        
        val threadCount = Runtime.getRuntime().availableProcessors()
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationSeconds * 1000
        
        // 创建与CPU核心数相同的线程，每个线程都执行密集计算
        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    while (System.currentTimeMillis() < endTime && isRunning) {
                        // 执行密集计算
                        var result = 0.0
                        for (i in 0 until 10000000) {
                            result += sqrt(i.toDouble()) * Math.sin(i.toDouble())
                        }
                        
                        // 只在一个线程中更新进度
                        if (threadId == 0) {
                            val currentTime = System.currentTimeMillis()
                            val elapsedTime = currentTime - startTime
                            val totalTime = durationSeconds * 1000
                            val progress = elapsedTime.toFloat() / totalTime
                            
                            if (currentTime % 5000 < 100) { // 大约每5秒更新一次
                                val progressMessage = "温度升高测试进行中，已完成: ${(progress * 100).toInt()}%"
                                logInfo(progressMessage)
                                mainHandler.post {
                                    logCallback?.onTestProgress(progress, progressMessage)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (threadId == 0) {
                        val errorMessage = "温度升高测试异常: ${e.message}"
                        logInfo(errorMessage)
                        mainHandler.post {
                            logCallback?.onTestCompleted(false, errorMessage)
                        }
                    }
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            if (isRunning) {
                isRunning = false
                val resultMessage = "温度升高测试结束"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "线程数: $threadCount, 持续时间: ${durationSeconds}秒")
                }
            }
        }, durationSeconds * 1000L)
    }
    
    // 停止所有测试
    fun stopAllTests() {
        if (!isRunning) return
        
        isRunning = false
        logInfo("停止所有CPU测试")
        mainHandler.post {
            logCallback?.onTestCompleted(true, "已停止所有测试")
        }
    }
    
    // 在Activity/Fragment销毁时调用
    fun onDestroy() {
        isRunning = false
        executor.shutdown()
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}
