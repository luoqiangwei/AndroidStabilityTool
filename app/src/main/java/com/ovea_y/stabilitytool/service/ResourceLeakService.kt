package com.ovea_y.stabilitytool.service

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.ovea_y.stabilitytool.R
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 资源泄漏测试类型
 * - 1. 窗口泄漏
 * - 2. Activity泄漏
 * - 3. PendingIntent泄漏
 * - 4. 闹钟泄漏
 * - 5. Binder代理全局引用泄漏
 * - 6. 任务调度器泄漏
 * - 7. Handler泄漏
 * - 8. 线程泄漏
 * - 9. Context泄漏
 */
class ResourceLeakService(private val context: Context) {
    private val TAG = "ResourceLeakService"
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()
    
    // 用于存储泄漏资源的容器
    companion object {
        private val leakedWindows = ArrayList<View>()
        private val leakedPendingIntents = ArrayList<PendingIntent>()
        private val leakedAlarms = ArrayList<PendingIntent>()
        private val leakedJobs = ArrayList<Int>()
        private val leakedThreads = ArrayList<Thread>()
        private val leakedHandlers = ArrayList<Handler>()
        private val leakedBinders = ArrayList<IBinder>()
        private val leakedContexts = ArrayList<Context>()
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
    
    // 1. 窗口泄漏测试
    fun causeWindowLeak(count: Int = 10) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.window_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.window_leak_test))
        
        // 检查是否有悬浮窗权限
        if (!Settings.canDrawOverlays(context)) {
            logInfo(context.getString(R.string.window_leak_permission_error))
            mainHandler.post {
                logCallback?.onTestCompleted(
                    false, 
                    context.getString(R.string.window_leak_permission_error), 
                    context.getString(R.string.window_leak_permission_error_detail)
                )
            }
            isRunning = false
            return
        }
        
        thread {
            try {
                var leakedCount = 0
                
                while (leakedCount < count && isRunning) {
                    // 在主线程创建窗口
                    mainHandler.post {
                        try {
                            // 创建一个新的窗口
                            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                            val view = TextView(context)
                            view.text = "Leaked Window #${leakedCount + 1}"
                            
                            val params = WindowManager.LayoutParams(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                android.graphics.PixelFormat.TRANSLUCENT
                            )
                            params.gravity = Gravity.TOP or Gravity.START
                            params.x = 100
                            params.y = 100 + (leakedCount * 10)
                            
                            // 添加窗口但不保存引用以便移除
                            windowManager.addView(view, params)
                            
                            // 保存到泄漏列表中
                            leakedWindows.add(view)
                        } catch (e: Exception) {
                            logInfo(context.getString(R.string.window_leak_create_failed, e.message))
                        }
                    }
                    
                    leakedCount++
                    val progress = leakedCount.toFloat() / count
                    
                    val progressMessage = context.getString(R.string.window_leak_progress, leakedCount, count)
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(500)
                }
                
                val resultMessage = context.getString(R.string.window_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.window_leak_count, leakedWindows.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.window_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 2. PendingIntent泄漏测试
    fun causePendingIntentLeak(count: Int = 100) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.pending_intent_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_pending_intent))
        
        thread {
            try {
                var leakedCount = 0
                
                while (leakedCount < count && isRunning) {
                    // 创建一个新的PendingIntent
                    val intent = Intent(context, context.javaClass)
                    intent.action = "com.ovea_y.stabilitytool.LEAK_ACTION_${System.currentTimeMillis()}"
                    
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        leakedCount,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    // 保存到泄漏列表中
                    leakedPendingIntents.add(pendingIntent)
                    
                    leakedCount++
                    val progress = leakedCount.toFloat() / count
                    
                    if (leakedCount % 10 == 0) {
                        val progressMessage = context.getString(R.string.pending_intent_leak_progress, leakedCount, count)
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(50)
                }
                
                val resultMessage = context.getString(R.string.pending_intent_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.pending_intent_leak_count, leakedPendingIntents.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.pending_intent_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 3. 闹钟泄漏测试
    fun causeAlarmLeak(count: Int = 50) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.alarm_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_alarm))
        
        thread {
            try {
                var leakedCount = 0
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                
                while (leakedCount < count && isRunning) {
                    // 创建一个新的PendingIntent用于闹钟
                    val intent = Intent(context, context.javaClass)
                    intent.action = "com.ovea_y.stabilitytool.ALARM_ACTION_${System.currentTimeMillis()}"
                    
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        1000 + leakedCount,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    // 设置一个不会触发的闹钟（设置在很远的将来）
                    val triggerTime = SystemClock.elapsedRealtime() + 24 * 60 * 60 * 1000 // 24小时后
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerTime, pendingIntent)
                    
                    // 保存到泄漏列表中
                    leakedAlarms.add(pendingIntent)
                    
                    leakedCount++
                    val progress = leakedCount.toFloat() / count
                    
                    if (leakedCount % 5 == 0) {
                        val progressMessage = context.getString(R.string.alarm_leak_progress, leakedCount, count)
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(100)
                }
                
                val resultMessage = context.getString(R.string.alarm_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.alarm_leak_count, leakedAlarms.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.alarm_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 4. Binder代理全局引用泄漏测试
    fun causeBinderProxyLeak(count: Int = 50) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.binder_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_binder_proxy))
        
        thread {
            try {
                var leakedCount = 0
                
                while (leakedCount < count && isRunning) {
                    // 创建一个新的Binder对象
                    val binder = Binder()
                    
                    // 保存到泄漏列表中
                    leakedBinders.add(binder)
                    
                    leakedCount++
                    val progress = leakedCount.toFloat() / count
                    
                    if (leakedCount % 5 == 0) {
                        val progressMessage = context.getString(R.string.binder_leak_progress, leakedCount, count)
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(100)
                }
                
                val resultMessage = context.getString(R.string.binder_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.binder_leak_count, leakedBinders.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.binder_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 5. 任务调度器泄漏测试
    fun causeJobSchedulerLeak(count: Int = 20) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.job_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_job))
        
        thread {
            try {
                var leakedCount = 0
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                
                // 检查是否有RECEIVE_BOOT_COMPLETED权限
                val hasPersistPermission = context.packageManager.checkPermission(
                    android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
                    context.packageName
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                while (leakedCount < count && isRunning) {
                    val jobId = 1000 + leakedCount
                    
                    try {
                        // 使用我们自己的DummyJobService
                        val componentName = ComponentName(context.packageName, "com.ovea_y.stabilitytool.service.DummyJobService")
                        val jobInfoBuilder = JobInfo.Builder(jobId, componentName)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setMinimumLatency(24 * 60 * 60 * 1000L) // 24小时后执行
                        
                        // 只有在有权限的情况下才设置setPersisted
                        if (hasPersistPermission) {
                            jobInfoBuilder.setPersisted(true)
                        } else if (leakedCount == 0) {
                            // 只在第一次循环时记录日志
                            logInfo(context.getString(R.string.missing_boot_permission))
                        }
                        
                        val jobInfo = jobInfoBuilder.build()
                        
                        // 调度任务
                        val result = jobScheduler.schedule(jobInfo)
                        if (result == JobScheduler.RESULT_SUCCESS) {
                            // 保存到泄漏列表中
                            leakedJobs.add(jobId)
                            
                            leakedCount++
                            val progress = leakedCount.toFloat() / count
                            
                            val progressMessage = context.getString(R.string.job_leak_progress, leakedCount, count)
                            logInfo(progressMessage)
                            mainHandler.post {
                                logCallback?.onTestProgress(progress, progressMessage)
                            }
                        } else {
                            logInfo(context.getString(R.string.job_leak_schedule_failed, result))
                        }
                    } catch (e: Exception) {
                        logInfo(context.getString(R.string.job_leak_schedule_exception, e.message))
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(200)
                }
                
                val resultMessage = context.getString(R.string.job_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.job_leak_count, leakedJobs.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.job_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 6. Handler泄漏测试
    fun causeHandlerLeak(count: Int = 30) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.handler_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_handler))
        
        thread {
            try {
                var leakedCount = 0
                
                while (leakedCount < count && isRunning) {
                    // 创建一个新的Handler
                    val handler = Handler(Looper.getMainLooper())
                    
                    // 设置一个长时间后执行的任务
                    handler.postDelayed({
                        logInfo(context.getString(R.string.handler_leak_message))
                    }, 24 * 60 * 60 * 1000L) // 24小时后执行
                    
                    // 保存到泄漏列表中
                    leakedHandlers.add(handler)
                    
                    leakedCount++
                    val progress = leakedCount.toFloat() / count
                    
                    if (leakedCount % 5 == 0) {
                        val progressMessage = context.getString(R.string.handler_leak_progress, leakedCount, count)
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(100)
                }
                
                val resultMessage = context.getString(R.string.handler_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.handler_leak_count, leakedHandlers.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.handler_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 7. 线程泄漏测试
    fun causeThreadLeak(count: Int = 50) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.thread_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_thread))
        
        thread {
            try {
                var leakedCount = 0
                
                while (leakedCount < count && isRunning) {
                    // 创建一个新的线程，但不会结束
                    val leakedThread = Thread {
                        try {
                            // 线程永远不会结束
                            while (true) {
                                Thread.sleep(10000)
                            }
                        } catch (e: InterruptedException) {
                            // 忽略中断异常
                        }
                    }
                    leakedThread.name = "LeakedThread-$leakedCount"
                    leakedThread.start()
                    
                    // 保存到泄漏列表中
                    leakedThreads.add(leakedThread)
                    
                    leakedCount++
                    val progress = leakedCount.toFloat() / count
                    
                    if (leakedCount % 5 == 0) {
                        val progressMessage = context.getString(R.string.thread_leak_progress, leakedCount, count)
                        logInfo(progressMessage)
                        mainHandler.post {
                            logCallback?.onTestProgress(progress, progressMessage)
                        }
                    }
                    
                    // 短暂休眠，避免创建线程过快
                    Thread.sleep(100)
                }
                
                val resultMessage = context.getString(R.string.thread_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.thread_leak_count, leakedThreads.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.thread_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 8. Context泄漏测试
    fun causeContextLeak(count: Int = 20) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.context_leak_start, count))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_context))
        
        thread {
            try {
                var leakedCount = 0
                
                while (leakedCount < count && isRunning) {
                    // 创建一个新的Context包装
                    val newContext = context.createConfigurationContext(context.resources.configuration)
                    
                    // 保存到泄漏列表中
                    leakedContexts.add(newContext)
                    
                    leakedCount++
                    val progress = leakedCount.toFloat() / count
                    
                    val progressMessage = context.getString(R.string.context_leak_progress, leakedCount, count)
                    logInfo(progressMessage)
                    mainHandler.post {
                        logCallback?.onTestProgress(progress, progressMessage)
                    }
                    
                    // 短暂休眠，避免UI卡顿
                    Thread.sleep(200)
                }
                
                val resultMessage = context.getString(R.string.context_leak_complete, leakedCount)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.context_leak_count, leakedContexts.size))
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.context_leak_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    // 9. Activity泄漏测试（需要在Activity中调用）
    fun causeActivityLeak(activity: Activity) {
        if (isRunning) return
        isRunning = true
        
        logInfo(context.getString(R.string.activity_leak_start))
        logCallback?.onTestStarted(context.getString(R.string.resource_leak_activity))
        
        // 创建一个静态引用，防止Activity被回收
        val activityRef = activity
        
        // 创建一个不会结束的线程，持有Activity引用
        val thread = Thread {
            try {
                // 持有Activity引用
                val activityName = activityRef.javaClass.simpleName
                logInfo(context.getString(R.string.activity_leak_holding, activityName))
                
                // 创建一个大数组，增加内存占用
                val largeArray = Array(100) { ByteArray(1024 * 1024) } // 100MB
                
                // 使用Activity的引用
                for (i in 0 until 10) {
                    if (!isRunning) break
                    
                    // 在主线程上执行操作，确保持有Activity引用
                    mainHandler.post {
                        try {
                            // 获取Activity的一些信息，确保引用被使用
                            val title = activityRef.title
                            val packageName = activityRef.packageName
                            logInfo(context.getString(R.string.activity_leak_info, title, packageName))
                        } catch (e: Exception) {
                            logInfo(context.getString(R.string.activity_leak_info_failed, e.message))
                        }
                    }
                    
                    Thread.sleep(1000)
                }
                
                // 线程永远不会结束，除非手动停止
                while (isRunning) {
                    Thread.sleep(1000)
                    
                    // 定期使用Activity引用，确保引用被保持
                    mainHandler.post {
                        try {
                            if (activityRef.isFinishing) {
                                logInfo(context.getString(R.string.activity_leak_finished))
                            } else {
                                logInfo(context.getString(R.string.activity_leak_alive))
                            }
                        } catch (e: Exception) {
                            logInfo(context.getString(R.string.activity_leak_check_failed, e.message))
                        }
                    }
                }
            } catch (e: InterruptedException) {
                // 忽略中断异常
                logInfo(context.getString(R.string.activity_leak_thread_interrupted))
            } catch (e: Exception) {
                logInfo(context.getString(R.string.activity_leak_error, e.message))
            }
        }
        thread.name = "ActivityLeakThread"
        thread.start()
        
        // 保存到泄漏线程列表中
        leakedThreads.add(thread)
        
        val resultMessage = context.getString(R.string.activity_leak_complete)
        logInfo(resultMessage)
        mainHandler.post {
            logCallback?.onTestProgress(1.0f, context.getString(R.string.activity_leak_created))
            logCallback?.onTestCompleted(true, resultMessage, context.getString(R.string.activity_leak_detail, activity.javaClass.simpleName))
        }
    }
    
    // 清理资源泄漏
    fun clearResourceLeaks() {
        thread {
            try {
                logInfo(context.getString(R.string.clear_leaks_start))
                
                // 1. 清理窗口泄漏
                if (leakedWindows.isNotEmpty()) {
                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    for (view in leakedWindows) {
                        try {
                            windowManager.removeView(view)
                        } catch (e: Exception) {
                            logInfo(context.getString(R.string.clear_window_failed, e.message))
                        }
                    }
                    logInfo(context.getString(R.string.clear_window_complete, leakedWindows.size))
                    leakedWindows.clear()
                }
                
                // 2. 清理PendingIntent泄漏
                if (leakedPendingIntents.isNotEmpty()) {
                    for (pendingIntent in leakedPendingIntents) {
                        pendingIntent.cancel()
                    }
                    logInfo(context.getString(R.string.clear_pending_intent_complete, leakedPendingIntents.size))
                    leakedPendingIntents.clear()
                }
                
                // 3. 清理闹钟泄漏
                if (leakedAlarms.isNotEmpty()) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    for (pendingIntent in leakedAlarms) {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    }
                    logInfo(context.getString(R.string.clear_alarm_complete, leakedAlarms.size))
                    leakedAlarms.clear()
                }
                
                // 4. 清理任务调度器泄漏
                if (leakedJobs.isNotEmpty()) {
                    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    for (jobId in leakedJobs) {
                        jobScheduler.cancel(jobId)
                    }
                    logInfo(context.getString(R.string.clear_job_complete, leakedJobs.size))
                    leakedJobs.clear()
                }
                
                // 5. 清理线程泄漏
                if (leakedThreads.isNotEmpty()) {
                    for (thread in leakedThreads) {
                        thread.interrupt()
                    }
                    logInfo(context.getString(R.string.clear_thread_complete, leakedThreads.size))
                    leakedThreads.clear()
                }
                
                // 6. 清理Handler泄漏
                if (leakedHandlers.isNotEmpty()) {
                    for (handler in leakedHandlers) {
                        handler.removeCallbacksAndMessages(null)
                    }
                    logInfo(context.getString(R.string.clear_handler_complete, leakedHandlers.size))
                    leakedHandlers.clear()
                }
                
                // 7. 清理Binder代理泄漏
                leakedBinders.clear()
                logInfo(context.getString(R.string.clear_binder_complete))
                
                // 8. 清理Context泄漏
                leakedContexts.clear()
                logInfo(context.getString(R.string.clear_context_complete))
                
                // 触发垃圾回收
                System.gc()
                
                val resultMessage = context.getString(R.string.clear_leaks_complete)
                logInfo(resultMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(true, resultMessage)
                }
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.clear_leaks_error, e.message)
                logInfo(errorMessage)
                mainHandler.post {
                    logCallback?.onTestCompleted(false, errorMessage)
                }
            }
        }
    }
    
    // 停止所有测试
    fun stopAllTests() {
        if (!isRunning) return
        
        isRunning = false
        logInfo(context.getString(R.string.stop_all_resource_tests))
        mainHandler.post {
            logCallback?.onTestCompleted(true, context.getString(R.string.tests_stopped))
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