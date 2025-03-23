package com.ovea_y.stabilitytool.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * 内存测试类型
 * - 1. 内存泄漏
 * - 2. 内存抖动
 * - 3. 大对象分配
 * - 4. 内存碎片化
 * - 5. 内存耗尽（OOM）
 * - 6. 缓存过度使用
 */
class MemoryTestService {
    private val TAG = "MemoryTestService"
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()
    
    // 用于模拟内存泄漏的静态列表
    companion object {
        private val leakedObjects = ArrayList<ByteArray>()
    }
    
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
    
    // 1. 内存泄漏测试
    fun causeMemoryLeak(leakSizeMB: Int = 100, leakStepMB: Int = 10) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始内存泄漏测试，目标泄漏: ${leakSizeMB}MB")
        logCallback?.onTestStarted("内存泄漏测试")
        
        thread {
            try {
                var leakedMB = 0
                
                while (leakedMB < leakSizeMB && isRunning) {
                    // 每次分配leakStepMB MB的内存并保持引用
                    val bytes = ByteArray(leakStepMB * 1024 * 1024)
                    // 填充随机数据，确保内存被实际使用
                    Random.nextBytes(bytes)
                    // 添加到静态列表，模拟内存泄漏
                    leakedObjects.add(bytes)
                    
                    leakedMB += leakStepMB
                    val progress = leakedMB.toFloat() / leakSizeMB
                    
                    val progressMessage = "已泄漏 ${leakedMB}MB / ${leakSizeMB}MB 内存"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(500)
                }
                
                val resultMessage = "内存泄漏测试完成，总共泄漏: ${leakedMB}MB"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "泄漏对象数量: ${leakedObjects.size}")
                }
            } catch (e: Exception) {
                val errorMessage = "内存泄漏测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 2. 内存抖动测试
    fun causeMemoryChurn(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始内存抖动测试，持续: ${durationSeconds}秒")
        logCallback?.onTestStarted("内存抖动测试")
        
        thread {
            try {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + durationSeconds * 1000
                var iterationCount = 0
                
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 创建大量临时对象
                    val tempList = ArrayList<ByteArray>(1000)
                    for (i in 0 until 1000) {
                        val bytes = ByteArray(1024) // 1KB
                        Random.nextBytes(bytes)
                        tempList.add(bytes)
                    }
                    
                    // 对临时对象进行一些操作
                    var totalSize = 0
                    for (bytes in tempList) {
                        totalSize += bytes.size
                    }
                    
                    // 清空列表，释放对象
                    tempList.clear()
                    
                    iterationCount++
                    
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - startTime
                    val totalTime = durationSeconds * 1000
                    val progress = elapsedTime.toFloat() / totalTime
                    
                    if (iterationCount % 10 == 0) {
                        val progressMessage = "内存抖动测试进行中，已完成 $iterationCount 次迭代"
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠
                    Thread.sleep(50)
                }
                
                val resultMessage = "内存抖动测试完成，总共执行 $iterationCount 次迭代"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
                }
            } catch (e: Exception) {
                val errorMessage = "内存抖动测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 3. 大对象分配测试
    fun causeLargeObjectAllocation(sizeMB: Int = 200) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始大对象分配测试，目标大小: ${sizeMB}MB")
        logCallback?.onTestStarted("大对象分配测试")
        
        thread {
            try {
                // 分配一个大对象
                val largeObject = ByteArray(sizeMB * 1024 * 1024)
                
                // 填充随机数据，确保内存被实际使用
                logInfo("正在填充大对象数据...")
                mainHandler.post {
                    logCallback?.onTestProgress(0.5f, "正在填充大对象数据...")
                }
                
                // 分块填充，避免UI卡顿
                val blockSize = 10 * 1024 * 1024 // 10MB
                val blockCount = sizeMB / 10
                
                for (i in 0 until blockCount) {
                    val startIndex = i * blockSize
                    val endIndex = minOf(startIndex + blockSize, largeObject.size)
                    Random.nextBytes(largeObject.sliceArray(startIndex until endIndex))
                    
                    val progress = 0.5f + (i.toFloat() / blockCount) * 0.5f
                    val progressMessage = "已填充 ${(i + 1) * 10}MB / ${sizeMB}MB"
                    
                    if (i % 5 == 0) {
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(100)
                }
                
                // 保持对象引用一段时间
                Thread.sleep(5000)
                
                val resultMessage = "大对象分配测试完成，对象大小: ${sizeMB}MB"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage)
                }
            } catch (e: OutOfMemoryError) {
                val errorMessage = "大对象分配测试失败: 内存不足 (OOM)"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage, "请尝试减小对象大小")
                }
            } catch (e: Exception) {
                val errorMessage = "大对象分配测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 4. 内存碎片化测试
    fun causeMemoryFragmentation(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始内存碎片化测试，持续: ${durationSeconds}秒")
        logCallback?.onTestStarted("内存碎片化测试")
        
        thread {
            try {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + durationSeconds * 1000
                
                // 创建不同大小的对象列表
                val objectLists = ArrayList<ArrayList<ByteArray>>()
                for (i in 0 until 10) {
                    objectLists.add(ArrayList())
                }
                
                var iterationCount = 0
                
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 随机选择一个列表
                    val listIndex = Random.nextInt(objectLists.size)
                    val list = objectLists[listIndex]
                    
                    // 随机决定是添加还是删除对象
                    if (list.isEmpty() || Random.nextBoolean()) {
                        // 添加一个随机大小的对象
                        val objectSize = Random.nextInt(1, 100) * 1024 // 1KB到100KB
                        val bytes = ByteArray(objectSize)
                        Random.nextBytes(bytes)
                        list.add(bytes)
                    } else {
                        // 删除一个随机对象
                        val objectIndex = Random.nextInt(list.size)
                        list.removeAt(objectIndex)
                    }
                    
                    iterationCount++
                    
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - startTime
                    val totalTime = durationSeconds * 1000
                    val progress = elapsedTime.toFloat() / totalTime
                    
                    if (iterationCount % 1000 == 0) {
                        // 计算当前持有的内存总量
                        var totalMemory = 0L
                        var totalObjects = 0
                        for (objList in objectLists) {
                            totalObjects += objList.size
                            for (obj in objList) {
                                totalMemory += obj.size
                            }
                        }
                        
                        val memoryMB = totalMemory / (1024 * 1024)
                        val progressMessage = "内存碎片化测试进行中，当前持有 $totalObjects 个对象，总内存: ${memoryMB}MB"
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠
                    if (iterationCount % 100 == 0) {
                        Thread.sleep(10)
                    }
                }
                
                // 计算最终状态
                var totalMemory = 0L
                var totalObjects = 0
                for (objList in objectLists) {
                    totalObjects += objList.size
                    for (obj in objList) {
                        totalMemory += obj.size
                    }
                }
                
                val memoryMB = totalMemory / (1024 * 1024)
                val resultMessage = "内存碎片化测试完成，最终持有 $totalObjects 个对象，总内存: ${memoryMB}MB"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
                }
                
                // 清理对象
                for (list in objectLists) {
                    list.clear()
                }
                objectLists.clear()
            } catch (e: Exception) {
                val errorMessage = "内存碎片化测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 5. 内存耗尽测试（OOM）
    fun causeOutOfMemoryError() {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始OOM异常测试")
        logCallback?.onTestStarted("OOM异常测试")
        
        thread {
            try {
                val largeObjects = ArrayList<ByteArray>()
                var allocatedMB = 0
                
                logInfo("开始分配大量内存...")
                
                // 不断分配内存直到OOM
                while (isRunning) {
                    val blockSize = 50 * 1024 * 1024 // 50MB
                    val bytes = ByteArray(blockSize)
                    // 填充一些数据，确保内存被实际使用
                    Random.nextBytes(bytes.sliceArray(0 until 1024))
                    largeObjects.add(bytes)
                    
                    allocatedMB += 50
                    val progressMessage = "已分配 ${allocatedMB}MB 内存"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(minOf(allocatedMB.toFloat() / 1000, 0.99f), progressMessage)
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(200)
                }
            } catch (e: OutOfMemoryError) {
                val resultMessage = "成功触发OOM异常"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "异常信息: ${e.message}")
                }
            } catch (e: Exception) {
                val errorMessage = "OOM异常测试出现其他异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
                // 尝试释放一些内存
                System.gc()
            }
        }
    }
    
    // 6. 缓存过度使用测试
    fun causeCacheAbuse(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始缓存过度使用测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("缓存过度使用测试")
        
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        // 创建一个大型LRU缓存
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val cacheSize = (maxMemory / 4).toInt() // 使用1/4的可用内存
        
        val memoryCache = object : LruCache<String, ByteArray>(cacheSize) {
            override fun sizeOf(key: String, value: ByteArray): Int {
                // 返回对象大小（KB）
                return value.size / 1024
            }
        }
        
        thread {
            try {
                var counter = 0
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 不断向缓存中添加对象
                    val key = "item_${counter++}"
                    val randomSize = (100 + Math.random() * 900).toInt() // 100KB到1000KB
                    val value = ByteArray(randomSize * 1024)
                    
                    memoryCache.put(key, value)
                    
                    if (counter % 100 == 0) {
                        val progressMessage = "缓存中已添加 $counter 个对象，当前缓存大小: ${memoryCache.size()}KB / ${memoryCache.maxSize()}KB"
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(counter.toFloat() / (durationSeconds * 100), progressMessage)
                        }
                    }
                    
                    Thread.sleep(10)
                }
                
                val resultMessage = "缓存过度使用测试完成，总共添加了 $counter 个对象"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
                }
            } catch (e: Exception) {
                val errorMessage = "缓存过度使用测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                memoryCache.evictAll() // 清空缓存
                isRunning = false
                logInfo("缓存过度使用测试结束")
            }
        }
    }
    
    // 7. 弱引用测试
    fun causeWeakReferenceTest(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始弱引用测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("弱引用测试")
        
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        val weakRefs = ArrayList<WeakReference<ByteArray>>()
        
        thread {
            try {
                // 用于最终统计的变量
                var finalAliveCount = 0
                
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 创建对象并只持有弱引用
                    repeat(1000) {
                        val data = ByteArray(100 * 1024) // 100KB
                        weakRefs.add(WeakReference(data))
                    }
                    
                    // 触发GC
                    System.gc()
                    
                    // 检查有多少弱引用被回收
                    val aliveCount = weakRefs.count { it.get() != null }
                    finalAliveCount = aliveCount // 更新最终统计变量
                    
                    val progressMessage = "弱引用测试: 总数=${weakRefs.size}, 存活数=$aliveCount"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(aliveCount.toFloat() / weakRefs.size, progressMessage)
                    }
                    
                    Thread.sleep(1000)
                }
                
                val resultMessage = "弱引用测试完成，总共创建了 ${weakRefs.size} 个对象，存活数: $finalAliveCount"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
                }
            } catch (e: Exception) {
                val errorMessage = "弱引用测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                weakRefs.clear()
                isRunning = false
                logInfo("弱引用测试结束")
            }
        }
    }
    
    // 8. 并发集合测试
    fun causeConcurrentCollectionTest(durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始并发集合测试，持续${durationSeconds}秒")
        logCallback?.onTestStarted("并发集合测试")
        
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        val map = ConcurrentHashMap<String, ByteArray>()
        
        // 创建多个线程同时操作集合
        repeat(4) { threadId ->
            thread {
                try {
                    var counter = 0
                    while (System.currentTimeMillis() < endTime && isRunning) {
                        // 添加大量数据
                        repeat(1000) {
                            val key = "thread_${threadId}_item_${counter++}"
                            map[key] = ByteArray(10 * 1024) // 10KB
                        }
                        
                        // 删除一些数据
                        val keysToRemove = map.keys.take(500)
                        keysToRemove.forEach { map.remove(it) }
                        
                        val progressMessage = "线程 $threadId: 当前Map大小 = ${map.size}"
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(counter.toFloat() / (durationSeconds * 100), progressMessage)
                        }
                        
                        Thread.sleep(100)
                    }
                    
                    val resultMessage = "并发集合测试完成，总共添加了 $counter 个对象"
                    logInfo(resultMessage)
                    mainHandler.post {
                        logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
                    }
                } catch (e: Exception) {
                    val errorMessage = "并发集合测试异常: ${e.message}"
                    logInfo(errorMessage)
                    mainHandler.post {
                        logCallback?.onTestCompleted(false, errorMessage)
                    }
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            isRunning = false
            map.clear()
            logInfo("并发集合测试结束")
        }, durationSeconds * 1000L)
    }
    
    // 清理内存泄漏
    fun clearMemoryLeak() {
        if (leakedObjects.isNotEmpty()) {
            val leakedMB = leakedObjects.sumOf { it.size } / (1024 * 1024)
            leakedObjects.clear()
            System.gc()
            
            val message = "已清理内存泄漏，释放约 ${leakedMB}MB 内存"
            logInfo(message)
            mainHandler.post {
                logCallback?.onTestCompleted(true, message)
            }
        } else {
            val message = "没有检测到内存泄漏"
            logInfo(message)
            mainHandler.post {
                logCallback?.onTestCompleted(true, message)
            }
        }
    }
    
    // 停止所有测试
    fun stopAllTests() {
        if (!isRunning) return
        
        isRunning = false
        logInfo("停止所有内存测试")
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

