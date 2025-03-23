package com.ovea_y.stabilitytool.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * 磁盘I/O测试类型
 * - 1. 顺序写入大文件
 * - 2. 顺序读取大文件
 * - 3. 随机读写小文件
 * - 4. 频繁创建删除文件
 * - 5. 文件碎片化
 * - 6. 并发读写
 * - 7. 文件系统填满
 */
class DiskTestService {
    private val TAG = "DiskTestService"
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()
    
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
    
    // 1. 顺序写入大文件测试
    fun causeSequentialWrite(context: Context, fileSizeMB: Int = 500) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始顺序写入大文件测试，文件大小: ${fileSizeMB}MB")
        logCallback?.onTestStarted("顺序写入大文件测试")
        
        thread {
            try {
                val file = File(context.filesDir, "large_sequential_write.dat")
                file.delete() // 确保文件不存在
                
                val buffer = ByteArray(1024 * 1024) // 1MB 缓冲区
                Random.nextBytes(buffer) // 填充随机数据
                
                val startTime = System.currentTimeMillis()
                
                FileOutputStream(file).use { output ->
                    for (i in 0 until fileSizeMB) {
                        output.write(buffer)
                        output.flush()
                        
                        val progress = (i + 1).toFloat() / fileSizeMB
                        
                        if (i % 10 == 0) {
                            val progressMessage = "已写入 ${i}MB / ${fileSizeMB}MB"
                            logInfo(progressMessage)
                            mainHandler.post {
                                logCallback?.onTestProgress(progress, progressMessage)
                            }
                        }
                        
                        if (!isRunning) break
                    }
                }
                
                val endTime = System.currentTimeMillis()
                val duration = (endTime - startTime) / 1000.0
                val speed = fileSizeMB / duration
                
                val resultMessage = "顺序写入完成，耗时: ${duration}秒，速度: ${speed}MB/s"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "文件大小: ${fileSizeMB}MB")
                }
            } catch (e: Exception) {
                val errorMessage = "顺序写入测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 2. 顺序读取大文件测试
    fun causeSequentialRead(context: Context, fileSizeMB: Int = 500) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始顺序读取大文件测试")
        logCallback?.onTestStarted("顺序读取大文件测试")
        
        // 首先确保文件存在
        thread {
            try {
                val file = File(context.filesDir, "large_sequential_read.dat")
                
                // 如果文件不存在或大小不对，先创建
                if (!file.exists() || file.length() < fileSizeMB * 1024 * 1024L) {
                    logInfo("文件不存在或大小不足，先创建文件")
                    
                    file.delete()
                    val buffer = ByteArray(1024 * 1024) // 1MB 缓冲区
                    Random.nextBytes(buffer) // 填充随机数据
                    
                    FileOutputStream(file).use { output ->
                        for (i in 0 until fileSizeMB) {
                            output.write(buffer)
                            if (i % 50 == 0) {
                                val createProgress = "创建文件: 已写入 ${i}MB / ${fileSizeMB}MB"
                                logInfo(createProgress)
                                mainHandler.post {
                                    logCallback?.onTestProgress(i.toFloat() / fileSizeMB / 2, createProgress)
                                }
                            }
                        }
                    }
                    logInfo("文件创建完成，大小: ${file.length() / (1024 * 1024)}MB")
                }
                
                // 开始读取测试
                val buffer = ByteArray(1024 * 1024) // 1MB 缓冲区
                val startTime = System.currentTimeMillis()
                
                FileInputStream(file).use { input ->
                    var bytesRead: Int
                    var totalRead = 0
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalRead += bytesRead
                        
                        val totalReadMB = totalRead / (1024 * 1024)
                        val progress = 0.5f + (totalReadMB.toFloat() / fileSizeMB / 2)
                        
                        if (totalRead % (50 * 1024 * 1024) == 0) {
                            val readProgress = "已读取 ${totalReadMB}MB"
                            logInfo(readProgress)
                            mainHandler.post {
                                logCallback?.onTestProgress(progress, readProgress)
                            }
                        }
                        
                        if (!isRunning) break
                    }
                }
                
                val endTime = System.currentTimeMillis()
                val duration = (endTime - startTime) / 1000.0
                val totalReadMB = file.length() / (1024 * 1024)
                val speed = totalReadMB / duration
                
                val resultMessage = "顺序读取完成，读取: ${totalReadMB}MB，耗时: ${duration}秒，速度: ${speed}MB/s"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "文件大小: ${totalReadMB}MB")
                }
            } catch (e: Exception) {
                val errorMessage = "顺序读取测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 3. 随机读写小文件测试
    fun causeRandomSmallFileIO(context: Context, fileCount: Int = 1000, durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始随机读写小文件测试，文件数: $fileCount，持续: ${durationSeconds}秒")
        logCallback?.onTestStarted("随机读写小文件测试")
        
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        // 创建测试目录
        val testDir = File(context.filesDir, "small_files_test")
        testDir.mkdirs()
        
        thread {
            try {
                // 先创建所有小文件
                for (i in 0 until fileCount) {
                    val file = File(testDir, "small_file_$i.dat")
                    FileOutputStream(file).use { output ->
                        val data = ByteArray(1024) // 1KB
                        Random.nextBytes(data)
                        output.write(data)
                    }
                    
                    val createProgress = i.toFloat() / fileCount * 0.3f
                    
                    if (i % 100 == 0) {
                        val createMessage = "已创建 $i / $fileCount 个小文件"
                        logInfo(createMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(createProgress, createMessage)
                        }
                    }
                    
                    if (!isRunning) return@thread
                }
                
                logInfo("小文件创建完成，开始随机读写测试")
                
                // 随机读写测试
                var readCount = 0
                var writeCount = 0
                val startTime = System.currentTimeMillis()
                
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 随机选择一个文件
                    val fileIndex = Random.nextInt(fileCount)
                    val file = File(testDir, "small_file_$fileIndex.dat")
                    
                    if (Random.nextBoolean()) {
                        // 读取文件
                        if (file.exists()) {
                            FileInputStream(file).use { input ->
                                val data = ByteArray(1024)
                                input.read(data)
                                readCount++
                            }
                        }
                    } else {
                        // 写入文件
                        FileOutputStream(file).use { output ->
                            val data = ByteArray(1024)
                            Random.nextBytes(data)
                            output.write(data)
                            writeCount++
                        }
                    }
                    
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - startTime
                    val totalTime = durationSeconds * 1000
                    val timeProgress = elapsedTime.toFloat() / totalTime
                    val progress = 0.3f + timeProgress * 0.7f
                    
                    if ((readCount + writeCount) % 1000 == 0) {
                        val progressMessage = "已执行 $readCount 次读取，$writeCount 次写入"
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                }
                
                val resultMessage = "随机读写小文件测试完成，总共执行 $readCount 次读取，$writeCount 次写入"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "文件数: $fileCount, 持续时间: ${durationSeconds}秒")
                }
            } catch (e: Exception) {
                val errorMessage = "随机读写小文件测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                // 清理文件
                testDir.deleteRecursively()
                isRunning = false
            }
        }
    }
    
    // 4. 频繁创建删除文件测试
    fun causeFrequentFileCreateDelete(context: Context, durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始频繁创建删除文件测试，持续: ${durationSeconds}秒")
        logCallback?.onTestStarted("频繁创建删除文件测试")
        
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        // 创建测试目录
        val testDir = File(context.filesDir, "create_delete_test")
        testDir.mkdirs()
        
        thread {
            try {
                var createCount = 0
                var deleteCount = 0
                val startTime = System.currentTimeMillis()
                
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 创建一批文件
                    val filesToCreate = 100
                    val files = ArrayList<File>(filesToCreate)
                    
                    for (i in 0 until filesToCreate) {
                        val file = File(testDir, "temp_file_${createCount + i}.dat")
                        FileOutputStream(file).use { output ->
                            val data = ByteArray(1024) // 1KB
                            Random.nextBytes(data)
                            output.write(data)
                        }
                        files.add(file)
                        createCount++
                    }
                    
                    // 删除这批文件
                    for (file in files) {
                        if (file.exists() && file.delete()) {
                            deleteCount++
                        }
                    }
                    
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - startTime
                    val totalTime = durationSeconds * 1000
                    val progress = elapsedTime.toFloat() / totalTime
                    
                    val progressMessage = "已创建 $createCount 个文件，删除 $deleteCount 个文件"
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                }
                
                val resultMessage = "频繁创建删除文件测试完成，总共创建 $createCount 个文件，删除 $deleteCount 个文件"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "持续时间: ${durationSeconds}秒")
                }
            } catch (e: Exception) {
                val errorMessage = "频繁创建删除文件测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                // 清理文件
                testDir.deleteRecursively()
                isRunning = false
            }
        }
    }
    
    // 5. 文件碎片化测试
    fun causeFileFragmentation(context: Context, durationSeconds: Int = 60) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始文件碎片化测试，持续: ${durationSeconds}秒")
        logCallback?.onTestStarted("文件碎片化测试")
        
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        thread {
            try {
                val file = File(context.filesDir, "fragmented_file.dat")
                file.delete()
                
                // 创建随机访问文件
                val raf = RandomAccessFile(file, "rw")
                
                // 设置文件大小为50MB
                val fileSize = 50 * 1024 * 1024L
                raf.setLength(fileSize)
                
                val buffer = ByteArray(4096) // 4KB 缓冲区
                var operationCount = 0
                val startTime = System.currentTimeMillis()
                
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 随机位置
                    val position = Random.nextLong(0, fileSize - buffer.size)
                    raf.seek(position)
                    
                    // 随机读或写
                    if (Random.nextBoolean()) {
                        // 读取
                        raf.read(buffer)
                    } else {
                        // 写入
                        Random.nextBytes(buffer)
                        raf.write(buffer)
                    }
                    
                    operationCount++
                    
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - startTime
                    val totalTime = durationSeconds * 1000
                    val progress = elapsedTime.toFloat() / totalTime
                    
                    if (operationCount % 1000 == 0) {
                        val progressMessage = "已执行 $operationCount 次随机读写操作"
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                }
                
                raf.close()
                val resultMessage = "文件碎片化测试完成，总共执行 $operationCount 次随机读写操作"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "文件大小: ${fileSize/(1024*1024)}MB, 持续时间: ${durationSeconds}秒")
                }
            } catch (e: Exception) {
                val errorMessage = "文件碎片化测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 6. 并发读写测试
    fun causeConcurrentIO(context: Context, durationSeconds: Int = 60, threadCount: Int = 4) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始并发读写测试，线程数: $threadCount，持续: ${durationSeconds}秒")
        logCallback?.onTestStarted("并发读写测试")
        
        // 创建测试目录
        val testDir = File(context.filesDir, "concurrent_io_test")
        testDir.mkdirs()
        
        // 创建一些测试文件
        val fileCount = 20
        for (i in 0 until fileCount) {
            val file = File(testDir, "test_file_$i.dat")
            FileOutputStream(file).use { output ->
                val data = ByteArray(1024 * 1024) // 1MB
                Random.nextBytes(data)
                output.write(data)
            }
        }
        
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationSeconds * 1000
        val totalOperations = Array(threadCount) { 0 }
        
        // 启动多个线程进行并发读写
        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    var operationCount = 0
                    
                    while (System.currentTimeMillis() < endTime && isRunning) {
                        // 随机选择一个文件
                        val fileIndex = Random.nextInt(fileCount)
                        val file = File(testDir, "test_file_$fileIndex.dat")
                        
                        if (Random.nextBoolean()) {
                            // 读取文件
                            FileInputStream(file).use { input ->
                                val buffer = ByteArray(1024 * 1024)
                                input.read(buffer)
                            }
                        } else {
                            // 写入文件
                            RandomAccessFile(file, "rw").use { raf ->
                                val position = Random.nextLong(0, file.length() - 1024)
                                raf.seek(position)
                                val buffer = ByteArray(1024)
                                Random.nextBytes(buffer)
                                raf.write(buffer)
                            }
                        }
                        
                        operationCount++
                        totalOperations[threadId] = operationCount
                        
                        if (operationCount % 100 == 0) {
                            val threadMessage = "线程 $threadId: 已执行 $operationCount 次IO操作"
                            logInfo(threadMessage)
                            
                            val currentTime = System.currentTimeMillis()
                            val elapsedTime = currentTime - startTime
                            val totalTime = durationSeconds * 1000
                            val progress = elapsedTime.toFloat() / totalTime
                            
                            val totalOps = totalOperations.sum()
                            val progressMessage = "总计执行 $totalOps 次IO操作"
                            
                            mainHandler.post {
                                logCallback?.onTestProgress(progress, progressMessage)
                            }
                        }
                    }
                    
                    logInfo("线程 $threadId: 并发IO测试完成，总共执行 $operationCount 次IO操作")
                } catch (e: Exception) {
                    logInfo("线程 $threadId: 并发IO测试异常: ${e.message}")
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            val totalOps = totalOperations.sum()
            val resultMessage = "并发IO测试结束，总共执行 $totalOps 次IO操作"
            logInfo(resultMessage)
            
            isRunning = false
            // 清理文件
            testDir.deleteRecursively()
            
            mainHandler.post {
                logCallback?.onTestCompleted(true, resultMessage, "线程数: $threadCount, 持续时间: ${durationSeconds}秒")
            }
        }, durationSeconds * 1000L)
    }
    
    // 7. 文件系统填满测试
    fun causeFilesystemFill(context: Context, targetPercentage: Int = 90) {
        if (isRunning) return
        isRunning = true
        
        logInfo("开始文件系统填满测试，目标使用率: $targetPercentage%")
        logCallback?.onTestStarted("文件系统填满测试")
        
        thread {
            try {
                val testDir = File(context.filesDir, "fill_test")
                testDir.mkdirs()
                
                val statFs = android.os.StatFs(context.filesDir.path)
                val blockSize = statFs.blockSizeLong
                val totalBlocks = statFs.blockCountLong
                val availableBlocks = statFs.availableBlocksLong
                
                val totalSpace = totalBlocks * blockSize
                val availableSpace = availableBlocks * blockSize
                val usedSpace = totalSpace - availableSpace
                val currentUsagePercentage = (usedSpace * 100 / totalSpace).toInt()
                
                val initialStatus = "当前存储状态: 总空间=${totalSpace/(1024*1024)}MB, " +
                        "可用空间=${availableSpace/(1024*1024)}MB, " +
                        "已用百分比=$currentUsagePercentage%"
                logInfo(initialStatus)
                mainHandler.post {
                    logCallback?.onTestProgress(currentUsagePercentage.toFloat() / targetPercentage, initialStatus)
                }
                
                if (currentUsagePercentage >= targetPercentage) {
                    val message = "当前使用率已达到目标，无需填充"
                    logInfo(message)
                    mainHandler.post {
                        logCallback?.onTestCompleted(true, message)
                    }
                    isRunning = false
                    return@thread
                }
                
                // 计算需要填充的空间
                val targetUsedSpace = totalSpace * targetPercentage / 100
                val spaceToFill = targetUsedSpace - usedSpace
                
                logInfo("需要填充 ${spaceToFill/(1024*1024)}MB 空间")
                
                // 每个文件100MB
                val fileSize = 100 * 1024 * 1024L
                val fileCount = (spaceToFill / fileSize).toInt() + 1
                
                val buffer = ByteArray(1024 * 1024) // 1MB 缓冲区
                Random.nextBytes(buffer)
                
                for (i in 0 until fileCount) {
                    if (!isRunning) break
                    
                    val file = File(testDir, "fill_file_$i.dat")
                    var bytesWritten = 0L
                    
                    try {
                        FileOutputStream(file).use { output ->
                            // 写入100MB或直到达到目标
                            while (bytesWritten < fileSize && isRunning) {
                                output.write(buffer)
                                bytesWritten += buffer.size
                                
                                // 每写入10MB检查一次
                                if (bytesWritten % (10 * 1024 * 1024) == 0L) {
                                    // 重新检查存储状态
                                    statFs.restat(context.filesDir.path)
                                    val newAvailableBlocks = statFs.availableBlocksLong
                                    val newAvailableSpace = newAvailableBlocks * blockSize
                                    val newUsedSpace = totalSpace - newAvailableSpace
                                    val newUsagePercentage = (newUsedSpace * 100 / totalSpace).toInt()
                                    
                                    val progressMessage = "已写入 ${bytesWritten/(1024*1024)}MB 到文件 $i, " +
                                            "当前使用率: $newUsagePercentage%"
                                    logInfo(progressMessage)
                                    mainHandler.post {
                                        logCallback?.onTestProgress(newUsagePercentage.toFloat() / targetPercentage, progressMessage)
                                    }
                                    
                                    if (newUsagePercentage >= targetPercentage) {
                                        val resultMessage = "已达到目标使用率，停止填充"
                                        logInfo(resultMessage)
                                        mainHandler.post {
                                            logCallback?.onTestCompleted(true, resultMessage, "目标使用率: $targetPercentage%, 实际使用率: $newUsagePercentage%")
                                        }
                                        isRunning = false
                                        break
                                    }
                                }
                            }
                        }
                        
                        logInfo("完成文件 $i 写入，大小: ${bytesWritten/(1024*1024)}MB")
                    } catch (e: Exception) {
                        val errorMessage = "写入文件 $i 时发生异常: ${e.message}"
                        logInfo(errorMessage)
                        mainHandler.post {
                            logCallback?.onTestCompleted(false, errorMessage)
                        }
                        break
                    }
                }
                
                // 最终检查
                statFs.restat(context.filesDir.path)
                val finalAvailableBlocks = statFs.availableBlocksLong
                val finalAvailableSpace = finalAvailableBlocks * blockSize
                val finalUsedSpace = totalSpace - finalAvailableSpace
                val finalUsagePercentage = (finalUsedSpace * 100 / totalSpace).toInt()
                
                val resultMessage = "文件系统填满测试完成，最终使用率: $finalUsagePercentage%"
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, "目标使用率: $targetPercentage%, 实际使用率: $finalUsagePercentage%")
                }
            } catch (e: Exception) {
                val errorMessage = "文件系统填满测试异常: ${e.message}"
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 停止所有测试
    fun stopAllTests() {
        isRunning = false
        logInfo("停止所有磁盘I/O测试")
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

