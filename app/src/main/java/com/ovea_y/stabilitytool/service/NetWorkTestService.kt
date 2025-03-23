package com.ovea_y.stabilitytool.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * 网络测试类型
 * - 1. 大文件下载
 * - 2. 并发连接
 * - 3. 网络延迟测试
 * - 4. 网络抖动测试
 * - 5. 网络带宽占用
 * - 6. 网络连接泄漏
 * - 7. DNS解析压力
 * - 8. 网络切换测试
 */
class NetWorkTestService {
    private val TAG = "NetWorkTestService"
    private var isRunning = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()
    
    // 测试用URL列表
    private val testUrls = listOf(
        "https://speed.cloudflare.com/__down?bytes=10000000", // 10MB
        "https://speed.hetzner.de/100MB.bin", // 100MB
        "https://proof.ovh.net/files/100Mb.dat", // 100MB
        "https://speed.cloudflare.com/__down?bytes=100000000" // 100MB
    )
    
    // 测试用域名列表
    private val testDomains = listOf(
        "www.google.com",
        "www.facebook.com",
        "www.amazon.com",
        "www.netflix.com",
        "www.microsoft.com",
        "www.apple.com",
        "www.twitter.com",
        "www.instagram.com",
        "www.linkedin.com",
        "www.github.com",
        "www.youtube.com",
        "www.reddit.com",
        "www.wikipedia.org",
        "www.baidu.com",
        "www.qq.com",
        "www.taobao.com",
        "www.yahoo.com",
        "www.ebay.com",
        "www.bing.com",
        "www.twitch.tv"
    )
    
    // 1. 大文件下载测试
    fun causeLargeFileDownload(context: Context, urlIndex: Int = 0) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        val url = testUrls[urlIndex % testUrls.size]
        Log.d(TAG, "开始大文件下载测试，URL: $url")
        
        thread {
            var connection: HttpURLConnection? = null
            var input: BufferedInputStream? = null
            var output: FileOutputStream? = null
            
            try {
                val startTime = System.currentTimeMillis()
                
                // 创建临时文件
                val file = File(context.cacheDir, "large_download_test.dat")
                if (file.exists()) file.delete()
                
                // 建立连接
                val urlObj = URL(url)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                
                // 获取文件大小
                val fileSize = connection.contentLength.toLong()
                Log.d(TAG, "文件大小: ${fileSize / (1024 * 1024)}MB")
                
                // 开始下载
                input = BufferedInputStream(connection.inputStream)
                output = FileOutputStream(file)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead: Long = 0
                var lastLogTime = System.currentTimeMillis()
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (!isRunning.get()) break
                    
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    
                    // 每秒记录一次进度
                    val now = System.currentTimeMillis()
                    if (now - lastLogTime > 1000) {
                        val progress = if (fileSize > 0) (totalRead * 100 / fileSize) else 0
                        val speed = totalRead / ((now - startTime) / 1000.0) / (1024 * 1024)
                        Log.d(TAG, "下载进度: $progress%, 速度: ${String.format("%.2f", speed)}MB/s")
                        lastLogTime = now
                    }
                }
                
                val endTime = System.currentTimeMillis()
                val duration = (endTime - startTime) / 1000.0
                val downloadedMB = totalRead / (1024.0 * 1024.0)
                val avgSpeed = downloadedMB / duration
                
                Log.d(TAG, "下载完成，总共下载: ${String.format("%.2f", downloadedMB)}MB, " +
                        "耗时: ${String.format("%.2f", duration)}秒, " +
                        "平均速度: ${String.format("%.2f", avgSpeed)}MB/s")
                
                // 删除临时文件
                file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "大文件下载测试异常: ${e.message}")
            } finally {
                try {
                    output?.close()
                    input?.close()
                    connection?.disconnect()
                } catch (e: IOException) {
                    Log.e(TAG, "关闭资源异常: ${e.message}")
                }
                
                isRunning.set(false)
                Log.d(TAG, "大文件下载测试结束")
            }
        }
    }
    
    // 2. 并发连接测试
    fun causeConcurrentConnections(connectionCount: Int = 100, durationSeconds: Int = 60) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        Log.d(TAG, "开始并发连接测试，连接数: $connectionCount，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        val activeConnections = AtomicInteger(0)
        val totalConnections = AtomicInteger(0)
        val failedConnections = AtomicInteger(0)
        
        // 启动监控线程
        thread {
            while (System.currentTimeMillis() < endTime && isRunning.get()) {
                Log.d(TAG, "并发连接状态 - 活跃: ${activeConnections.get()}, " +
                        "总计: ${totalConnections.get()}, 失败: ${failedConnections.get()}")
                Thread.sleep(1000)
            }
        }
        
        // 创建连接
        repeat(connectionCount) { connId ->
            executor.submit {
                try {
                    while (System.currentTimeMillis() < endTime && isRunning.get()) {
                        activeConnections.incrementAndGet()
                        totalConnections.incrementAndGet()
                        
                        try {
                            // 随机选择一个域名
                            val domain = testDomains[Random.nextInt(testDomains.size)]
                            val port = 80
                            
                            // 创建Socket连接
                            val socket = Socket(domain, port)
                            
                            // 保持连接一段时间
                            Thread.sleep(Random.nextLong(1000, 5000))
                            
                            // 关闭连接
                            socket.close()
                        } catch (e: Exception) {
                            failedConnections.incrementAndGet()
                            Log.e(TAG, "连接 $connId 失败: ${e.message}")
                        } finally {
                            activeConnections.decrementAndGet()
                        }
                        
                        // 随机等待一段时间再创建下一个连接
                        Thread.sleep(Random.nextLong(100, 1000))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "并发连接线程 $connId 异常: ${e.message}")
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            isRunning.set(false)
            Log.d(TAG, "并发连接测试结束，总计: ${totalConnections.get()}, 失败: ${failedConnections.get()}")
        }, durationSeconds * 1000L)
    }
    
    // 3. 网络延迟测试
    fun causeNetworkLatencyTest(targetHost: String = "8.8.8.8", count: Int = 100) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        Log.d(TAG, "开始网络延迟测试，目标: $targetHost，次数: $count")
        
        thread {
            try {
                val latencies = ArrayList<Long>()
                var successCount = 0
                var failCount = 0
                
                for (i in 0 until count) {
                    if (!isRunning.get()) break
                    
                    try {
                        val startTime = System.currentTimeMillis()
                        val reachable = InetAddress.getByName(targetHost).isReachable(3000)
                        val endTime = System.currentTimeMillis()
                        val latency = endTime - startTime
                        
                        if (reachable) {
                            latencies.add(latency)
                            successCount++
                            Log.d(TAG, "Ping #$i: $latency ms")
                        } else {
                            failCount++
                            Log.d(TAG, "Ping #$i: 超时")
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e(TAG, "Ping #$i 失败: ${e.message}")
                    }
                    
                    // 等待一段时间再发送下一个ping
                    Thread.sleep(100)
                }
                
                // 计算统计信息
                if (latencies.isNotEmpty()) {
                    val avgLatency = latencies.average()
                    val minLatency = latencies.minOrNull() ?: 0
                    val maxLatency = latencies.maxOrNull() ?: 0
                    
                    Log.d(TAG, "网络延迟测试结果 - 成功: $successCount, 失败: $failCount, " +
                            "平均延迟: ${String.format("%.2f", avgLatency)}ms, " +
                            "最小延迟: ${minLatency}ms, 最大延迟: ${maxLatency}ms")
                } else {
                    Log.d(TAG, "网络延迟测试结果 - 所有ping均失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "网络延迟测试异常: ${e.message}")
            } finally {
                isRunning.set(false)
                Log.d(TAG, "网络延迟测试结束")
            }
        }
    }
    
    // 4. 网络抖动测试
    fun causeNetworkJitterTest(targetHost: String = "8.8.8.8", durationSeconds: Int = 60) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        Log.d(TAG, "开始网络抖动测试，目标: $targetHost，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        thread {
            try {
                val latencies = ArrayList<Long>()
                var successCount = 0
                var failCount = 0
                var pingCount = 0
                
                while (System.currentTimeMillis() < endTime && isRunning.get()) {
                    try {
                        val startTime = System.currentTimeMillis()
                        val reachable = InetAddress.getByName(targetHost).isReachable(1000)
                        val endTime = System.currentTimeMillis()
                        val latency = endTime - startTime
                        
                        if (reachable) {
                            latencies.add(latency)
                            successCount++
                            Log.d(TAG, "Ping #$pingCount: $latency ms")
                        } else {
                            failCount++
                            Log.d(TAG, "Ping #$pingCount: 超时")
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e(TAG, "Ping #$pingCount 失败: ${e.message}")
                    }
                    
                    pingCount++
                    
                    // 每10次ping计算一次抖动
                    if (pingCount % 10 == 0 && latencies.size >= 2) {
                        val jitter = calculateJitter(latencies)
                        Log.d(TAG, "当前网络抖动: ${String.format("%.2f", jitter)}ms")
                        
                        // 只保留最近50个延迟值
                        if (latencies.size > 50) {
                            latencies.subList(0, latencies.size - 50).clear()
                        }
                    }
                    
                    // 随机等待时间，模拟不规则的ping
                    Thread.sleep(Random.nextLong(100, 500))
                }
                
                // 计算最终抖动
                if (latencies.size >= 2) {
                    val finalJitter = calculateJitter(latencies)
                    Log.d(TAG, "网络抖动测试结果 - 成功: $successCount, 失败: $failCount, " +
                            "平均抖动: ${String.format("%.2f", finalJitter)}ms")
                } else {
                    Log.d(TAG, "网络抖动测试结果 - 成功ping次数不足，无法计算抖动")
                }
            } catch (e: Exception) {
                Log.e(TAG, "网络抖动测试异常: ${e.message}")
            } finally {
                isRunning.set(false)
                Log.d(TAG, "网络抖动测试结束")
            }
        }
    }
    
    // 计算抖动（连续延迟差的平均值）
    private fun calculateJitter(latencies: List<Long>): Double {
        var sum = 0.0
        for (i in 1 until latencies.size) {
            sum += Math.abs(latencies[i] - latencies[i-1])
        }
        return sum / (latencies.size - 1)
    }
    
    // 5. 网络带宽占用测试
    fun causeBandwidthConsumption(context: Context, durationSeconds: Int = 60, threadCount: Int = 4) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        Log.d(TAG, "开始网络带宽占用测试，线程数: $threadCount，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        val totalDownloaded = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        
        // 启动监控线程
        thread {
            var lastLogTime = System.currentTimeMillis()
            var lastBytes = 0L
            
            while (System.currentTimeMillis() < endTime && isRunning.get()) {
                val now = System.currentTimeMillis()
                val currentBytes = totalDownloaded.get()
                val timeDiff = (now - lastLogTime) / 1000.0
                val bytesDiff = currentBytes - lastBytes
                val speed = bytesDiff / timeDiff / (1024 * 1024)
                
                Log.d(TAG, "带宽占用 - 当前速度: ${String.format("%.2f", speed)}MB/s, " +
                        "总计下载: ${currentBytes / (1024 * 1024)}MB")
                
                lastLogTime = now
                lastBytes = currentBytes
                
                Thread.sleep(1000)
            }
        }
        
        // 启动多个下载线程
        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    while (System.currentTimeMillis() < endTime && isRunning.get()) {
                        // 随机选择一个URL
                        val url = testUrls[Random.nextInt(testUrls.size)]
                        var connection: HttpURLConnection? = null
                        var input: BufferedInputStream? = null
                        
                        try {
                            // 建立连接
                            val urlObj = URL(url)
                            connection = urlObj.openConnection() as HttpURLConnection
                            connection.connectTimeout = 5000
                            connection.readTimeout = 10000
                            
                            // 开始下载
                            input = BufferedInputStream(connection.inputStream)
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!isRunning.get() || System.currentTimeMillis() >= endTime) break
                                totalDownloaded.addAndGet(bytesRead.toLong())
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "线程 $threadId 下载异常: ${e.message}")
                        } finally {
                            try {
                                input?.close()
                                connection?.disconnect()
                            } catch (e: IOException) {
                                Log.e(TAG, "关闭资源异常: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "带宽占用线程 $threadId 异常: ${e.message}")
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            val testDuration = (System.currentTimeMillis() - startTime) / 1000.0
            val totalMB = totalDownloaded.get() / (1024.0 * 1024.0)
            val avgSpeed = totalMB / testDuration
            
            isRunning.set(false)
            Log.d(TAG, "带宽占用测试结束，总计下载: ${String.format("%.2f", totalMB)}MB, " +
                    "平均速度: ${String.format("%.2f", avgSpeed)}MB/s")
        }, durationSeconds * 1000L)
    }
    
    // 6. 网络连接泄漏测试
    fun causeConnectionLeak(durationSeconds: Int = 60, leakCount: Int = 100) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        Log.d(TAG, "开始网络连接泄漏测试，泄漏连接数: $leakCount，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        // 存储所有泄漏的连接
        val leakedConnections = ArrayList<HttpURLConnection>()
        val leakedSockets = ArrayList<Socket>()
        
        thread {
            try {
                var connectionCount = 0
                
                while (System.currentTimeMillis() < endTime && isRunning.get() && connectionCount < leakCount) {
                    try {
                        // 创建HTTP连接但不关闭
                        val urlObj = URL(testUrls[Random.nextInt(testUrls.size)])
                        val connection = urlObj.openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.inputStream // 触发连接
                        
                        // 添加到泄漏列表
                        leakedConnections.add(connection)
                        connectionCount++
                        
                        // 创建Socket连接但不关闭
                        val domain = testDomains[Random.nextInt(testDomains.size)]
                        val socket = Socket(domain, 80)
                        leakedSockets.add(socket)
                        connectionCount++
                        
                        Log.d(TAG, "已创建 $connectionCount 个泄漏连接")
                        
                        // 短暂等待
                        Thread.sleep(100)
                    } catch (e: Exception) {
                        Log.e(TAG, "创建泄漏连接异常: ${e.message}")
                    }
                }
                
                Log.d(TAG, "已创建 ${leakedConnections.size} 个HTTP连接泄漏和 ${leakedSockets.size} 个Socket连接泄漏")
                
                // 等待测试结束
                while (System.currentTimeMillis() < endTime && isRunning.get()) {
                    Thread.sleep(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "网络连接泄漏测试异常: ${e.message}")
            } finally {
                // 测试结束，清理所有连接
                for (connection in leakedConnections) {
                    try {
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "关闭HTTP连接异常: ${e.message}")
                    }
                }
                
                for (socket in leakedSockets) {
                    try {
                        socket.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "关闭Socket异常: ${e.message}")
                    }
                }
                
                isRunning.set(false)
                Log.d(TAG, "网络连接泄漏测试结束，已清理所有连接")
            }
        }
    }
    
    // 7. DNS解析压力测试
    fun causeDnsResolutionStress(durationSeconds: Int = 60) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        Log.d(TAG, "开始DNS解析压力测试，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val totalTime = AtomicLong(0)
        
        // 启动多个线程进行DNS解析
        repeat(4) { threadId ->
            executor.submit {
                try {
                    while (System.currentTimeMillis() < endTime && isRunning.get()) {
                        // 随机选择一个域名
                        val domain = testDomains[Random.nextInt(testDomains.size)]
                        
                        try {
                            val startTime = System.currentTimeMillis()
                            val inetAddress = InetAddress.getByName(domain)
                            val endTime = System.currentTimeMillis()
                            val resolveTime = endTime - startTime
                            
                            successCount.incrementAndGet()
                            totalTime.addAndGet(resolveTime)
                            
                            Log.d(TAG, "线程 $threadId: 解析 $domain 成功，IP: ${inetAddress.hostAddress}，耗时: ${resolveTime}ms")
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                            Log.e(TAG, "线程 $threadId: 解析 $domain 失败: ${e.message}")
                        }
                        
                        // 短暂等待
                        Thread.sleep(Random.nextLong(10, 100))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "DNS解析线程 $threadId 异常: ${e.message}")
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            val totalSuccess = successCount.get()
            val totalFail = failCount.get()
            val avgTime = if (totalSuccess > 0) totalTime.get() / totalSuccess.toDouble() else 0.0
            
            isRunning.set(false)
            Log.d(TAG, "DNS解析压力测试结束，成功: $totalSuccess, 失败: $totalFail, " +
                    "平均解析时间: ${String.format("%.2f", avgTime)}ms")
        }, durationSeconds * 1000L)
    }
    
    // 8. 网络切换测试（需要在UI层实现，这里只提供检测网络状态的方法）
    fun checkNetworkStatus(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return when {
            capabilities == null -> "无网络连接"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "蓝牙"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "未知网络类型"
        }
    }
    
    // 停止所有测试
    fun stopAllTests() {
        isRunning.set(false)
        Log.d(TAG, "停止所有网络测试")
    }
    
    // 在Activity/Fragment销毁时调用
    fun onDestroy() {
        isRunning.set(false)
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

