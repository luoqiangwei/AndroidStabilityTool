package com.ovea_y.stabilitytool.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * 电源测试类型
 * - 1. CPU高负载耗电
 * - 2. 屏幕常亮耗电
 * - 3. GPS持续定位耗电
 * - 4. 传感器持续监听耗电
 * - 5. 网络持续传输耗电
 * - 6. 混合场景耗电
 * - 7. 后台持续运行耗电
 */
class PowerTestService {
    private val TAG = "PowerTestService"
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()
    
    // WakeLock对象，用于保持屏幕常亮
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 传感器相关
    private var sensorManager: SensorManager? = null
    private var sensorEventListener: SensorEventListener? = null
    
    // 位置相关
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    
    // 1. CPU高负载耗电测试
    fun causeCpuHighLoadPowerDrain(durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        Log.d(TAG, "开始CPU高负载耗电测试，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        // 启动多个线程执行密集计算
        val threadCount = Runtime.getRuntime().availableProcessors()
        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    while (System.currentTimeMillis() < endTime && isRunning) {
                        // 执行一些无意义但CPU密集的计算
                        var result = 0.0
                        for (i in 0 until 10000000) {
                            result += sqrt(i.toDouble()) * Math.sin(i.toDouble()) * Math.cos(i.toDouble())
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "CPU高负载线程 $threadId 异常: ${e.message}")
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            isRunning = false
            Log.d(TAG, "CPU高负载耗电测试结束")
        }, durationSeconds * 1000L)
    }
    
    // 2. 屏幕常亮耗电测试
    fun causeScreenOnPowerDrain(context: Context, durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        Log.d(TAG, "开始屏幕常亮耗电测试，持续: ${durationSeconds}秒")
        
        try {
            // 获取PowerManager
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // 创建WakeLock
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "StabilityTool:ScreenOnWakeLock"
            )
            
            // 获取WakeLock
            wakeLock?.acquire(durationSeconds * 1000L)
            
            Log.d(TAG, "已获取WakeLock，屏幕将保持常亮")
            
            // 设置定时器停止测试
            mainHandler.postDelayed({
                releaseWakeLock()
                isRunning = false
                Log.d(TAG, "屏幕常亮耗电测试结束")
            }, durationSeconds * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "屏幕常亮耗电测试异常: ${e.message}")
            releaseWakeLock()
            isRunning = false
        }
    }
    
    // 释放WakeLock
    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "已释放WakeLock")
            }
        } catch (e: Exception) {
            Log.e(TAG, "释放WakeLock异常: ${e.message}")
        }
    }
    
    // 3. GPS持续定位耗电测试
    fun causeGpsPowerDrain(context: Context, durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        Log.d(TAG, "开始GPS持续定位耗电测试，持续: ${durationSeconds}秒")
        
        try {
            // 获取LocationManager
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // 创建LocationListener
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "位置更新 - 经度: ${location.longitude}, 纬度: ${location.latitude}, " +
                            "精度: ${location.accuracy}m")
                }
                
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    Log.d(TAG, "位置状态变化: $provider, 状态: $status")
                }
                
                override fun onProviderEnabled(provider: String) {
                    Log.d(TAG, "位置提供者启用: $provider")
                }
                
                override fun onProviderDisabled(provider: String) {
                    Log.d(TAG, "位置提供者禁用: $provider")
                }
            }
            
            // 请求位置更新
            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 最小时间间隔，毫秒
                    0f,   // 最小距离变化，米
                    locationListener!!
                )
                Log.d(TAG, "已开始GPS位置监听")
            } catch (e: SecurityException) {
                Log.e(TAG, "请求位置更新权限被拒绝: ${e.message}")
                isRunning = false
                return
            }
            
            // 设置定时器停止测试
            mainHandler.postDelayed({
                stopLocationUpdates()
                isRunning = false
                Log.d(TAG, "GPS持续定位耗电测试结束")
            }, durationSeconds * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "GPS持续定位耗电测试异常: ${e.message}")
            stopLocationUpdates()
            isRunning = false
        }
    }
    
    // 停止位置更新
    private fun stopLocationUpdates() {
        try {
            locationListener?.let {
                locationManager?.removeUpdates(it)
                Log.d(TAG, "已停止GPS位置监听")
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止位置更新异常: ${e.message}")
        }
    }
    
    // 4. 传感器持续监听耗电测试
    fun causeSensorPowerDrain(context: Context, durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        Log.d(TAG, "开始传感器持续监听耗电测试，持续: ${durationSeconds}秒")
        
        try {
            // 获取SensorManager
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            
            // 创建SensorEventListener
            sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // 只记录部分传感器数据，避免日志过多
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        Log.d(TAG, "加速度传感器 - X: ${event.values[0]}, Y: ${event.values[1]}, Z: ${event.values[2]}")
                    }
                }
                
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    Log.d(TAG, "传感器精度变化: ${sensor.name}, 精度: $accuracy")
                }
            }
            
            // 注册多个传感器监听
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            val light = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
            
            // 注册所有可用的传感器
            accelerometer?.let {
                sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "已注册加速度传感器监听")
            }
            
            gyroscope?.let {
                sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "已注册陀螺仪传感器监听")
            }
            
            magnetometer?.let {
                sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "已注册磁力计传感器监听")
            }
            
            proximity?.let {
                sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
                Log.d(TAG, "已注册接近传感器监听")
            }
            
            light?.let {
                sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
                Log.d(TAG, "已注册光线传感器监听")
            }
            
            // 设置定时器停止测试
            mainHandler.postDelayed({
                unregisterSensors()
                isRunning = false
                Log.d(TAG, "传感器持续监听耗电测试结束")
            }, durationSeconds * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "传感器持续监听耗电测试异常: ${e.message}")
            unregisterSensors()
            isRunning = false
        }
    }
    
    // 注销传感器监听
    private fun unregisterSensors() {
        try {
            sensorEventListener?.let {
                sensorManager?.unregisterListener(it)
                Log.d(TAG, "已注销所有传感器监听")
            }
        } catch (e: Exception) {
            Log.e(TAG, "注销传感器监听异常: ${e.message}")
        }
    }
    
    // 5. 网络持续传输耗电测试
    fun causeNetworkPowerDrain(context: Context, durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        Log.d(TAG, "开始网络持续传输耗电测试，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        // 测试用URL列表
        val testUrls = listOf(
            "https://speed.cloudflare.com/__down?bytes=1000000", // 1MB
            "https://speed.cloudflare.com/__down?bytes=5000000", // 5MB
            "https://speed.cloudflare.com/__down?bytes=10000000" // 10MB
        )
        
        // 启动多个线程进行网络传输
        repeat(3) { threadId ->
            executor.submit {
                try {
                    var downloadCount = 0
                    
                    while (System.currentTimeMillis() < endTime && isRunning) {
                        try {
                            // 随机选择一个URL
                            val url = testUrls[threadId % testUrls.size]
                            val urlObj = java.net.URL(url)
                            val connection = urlObj.openConnection() as java.net.HttpURLConnection
                            connection.connectTimeout = 5000
                            connection.readTimeout = 10000
                            
                            // 读取数据
                            val input = connection.inputStream
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!isRunning || System.currentTimeMillis() >= endTime) break
                                totalRead += bytesRead
                            }
                            
                            input.close()
                            connection.disconnect()
                            
                            downloadCount++
                            Log.d(TAG, "线程 $threadId: 完成第 $downloadCount 次下载，大小: ${totalRead / (1024 * 1024)}MB")
                            
                            // 短暂等待
                            Thread.sleep(500)
                        } catch (e: Exception) {
                            Log.e(TAG, "线程 $threadId: 网络传输异常: ${e.message}")
                            Thread.sleep(1000) // 出错后等待一段时间再重试
                        }
                    }
                    
                    Log.d(TAG, "线程 $threadId: 网络传输结束，总共完成 $downloadCount 次下载")
                } catch (e: Exception) {
                    Log.e(TAG, "网络传输线程 $threadId 异常: ${e.message}")
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            isRunning = false
            Log.d(TAG, "网络持续传输耗电测试结束")
        }, durationSeconds * 1000L)
    }
    
    // 6. 混合场景耗电测试（结合CPU、传感器、网络）
    fun causeMixedScenarioPowerDrain(context: Context, durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        Log.d(TAG, "开始混合场景耗电测试，持续: ${durationSeconds}秒")
        
        // 启动CPU负载
        thread {
            val endTime = System.currentTimeMillis() + durationSeconds * 1000
            while (System.currentTimeMillis() < endTime && isRunning) {
                // 执行一些无意义但CPU密集的计算
                var result = 0.0
                for (i in 0 until 5000000) {
                    result += sqrt(i.toDouble()) * Math.sin(i.toDouble())
                }
            }
        }
        
        // 启动传感器监听
        try {
            // 获取SensorManager
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            
            // 创建SensorEventListener
            sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // 不记录日志，避免日志过多
                }
                
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // 不记录日志
                }
            }
            
            // 注册加速度传感器
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "已注册加速度传感器监听")
            }
        } catch (e: Exception) {
            Log.e(TAG, "注册传感器异常: ${e.message}")
        }
        
        // 启动网络传输
        thread {
            val endTime = System.currentTimeMillis() + durationSeconds * 1000
            val url = "https://speed.cloudflare.com/__down?bytes=1000000" // 1MB
            
            while (System.currentTimeMillis() < endTime && isRunning) {
                try {
                    val urlObj = java.net.URL(url)
                    val connection = urlObj.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 10000
                    
                    // 读取数据
                    val input = connection.inputStream
                    val buffer = ByteArray(8192)
                    while (input.read(buffer) != -1) {
                        if (!isRunning || System.currentTimeMillis() >= endTime) break
                    }
                    
                    input.close()
                    connection.disconnect()
                    
                    // 等待一段时间再进行下一次下载
                    Thread.sleep(2000)
                } catch (e: Exception) {
                    Log.e(TAG, "网络传输异常: ${e.message}")
                    Thread.sleep(1000)
                }
            }
        }
        
        // 设置定时器停止测试
        mainHandler.postDelayed({
            unregisterSensors()
            isRunning = false
            Log.d(TAG, "混合场景耗电测试结束")
        }, durationSeconds * 1000L)
    }
    
    // 7. 后台持续运行耗电测试
    fun causeBackgroundPowerDrain(durationSeconds: Int = 300) {
        if (isRunning) return
        isRunning = true
        
        Log.d(TAG, "开始后台持续运行耗电测试，持续: ${durationSeconds}秒")
        val endTime = System.currentTimeMillis() + durationSeconds * 1000
        
        // 启动一个低强度但持续的后台任务
        thread {
            try {
                var counter = 0
                
                while (System.currentTimeMillis() < endTime && isRunning) {
                    // 执行一些轻量级但持续的工作
                    counter++
                    
                    if (counter % 1000 == 0) {
                        // 每1000次循环记录一次日志
                        Log.d(TAG, "后台任务执行中，计数: $counter")
                    }
                    
                    // 短暂休眠，避免CPU使用率过高
                    Thread.sleep(10)
                }
                
                Log.d(TAG, "后台任务结束，总计数: $counter")
            } catch (e: Exception) {
                Log.e(TAG, "后台任务异常: ${e.message}")
            } finally {
                isRunning = false
                Log.d(TAG, "后台持续运行耗电测试结束")
            }
        }
    }
    
    // 停止所有测试
    fun stopAllTests() {
        isRunning = false
        releaseWakeLock()
        stopLocationUpdates()
        unregisterSensors()
        Log.d(TAG, "停止所有电源测试")
    }
    
    // 在Activity/Fragment销毁时调用
    fun onDestroy() {
        stopAllTests()
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

