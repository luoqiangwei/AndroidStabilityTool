package com.ovea_y.stabilitytool.service

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Process
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.ovea_y.stabilitytool.MainActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import kotlin.random.Random
import kotlin.system.exitProcess

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
    
    // 6. 广播接收器超时（10秒）
    class SlowBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 在onReceive中执行耗时操作，超过10秒会导致ANR
            Log.d("AnrSimulator", "trigger BroadCast anr")
            Thread.sleep(15000)
        }
    }
    
    fun causeBroadcastReceiverANR(context: Context) {
        // 注册广播接收器
//        val receiver = SlowBroadcastReceiver()
//        val filter = IntentFilter("com.ovea_y.stabilitytool.SLOW_BROADCAST")
        // 动态注册的广播无法触发
//        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        
        // 发送广播
        val intent = Intent("com.ovea_y.stabilitytool.SLOW_BROADCAST")
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        // 系统对隐式广播进行了限制，因此必须指定接受者，变成显式广播
        intent.setClassName(context.packageName, "com.ovea_y.stabilitytool.service.AnrSimulator\$SlowBroadcastReceiver")
        // sendBroadcast()是异步广播，系统不会严格按顺序等待接收器完成，可能仅导致主线程阻塞的普通ANR，而非广播ANR
        context.sendOrderedBroadcast(intent, null)
//        context.sendBroadcast(intent)
//        context.sendOrderedBroadcast(intent, null)
//        context.sendBroadcast(intent)
        Log.d("AnrSimulator", "BroadCast Send Finish")
        // 注意：实际测试中，需要在适当的时机调用context.unregisterReceiver(receiver)
    }

    fun scheduleRestart(context: Context, delayMs: Long = 1000) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val options = ActivityOptions.makeBasic().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent!!,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            options.toBundle()
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + delayMs, pendingIntent)
//        (context as? Activity)?.finishAffinity()
        exitProcess(0)
    }

    // 7. ContentProvider超时（10秒）
    class SlowContentProvider : ContentProvider() {
        override fun onCreate(): Boolean {
            // 读取标记决定是否阻塞
            val context = context ?: return false
            val prefs = context.getSharedPreferences("anr_config", Context.MODE_PRIVATE)
            if (prefs.getBoolean("onAnr", false) && prefs.getBoolean("block_oncreate", false)) {
                prefs
                    .edit()
                    .putBoolean("block_oncreate", false)
                    .putBoolean("onAnr", false)
                    .commit()
            } else {
                prefs
                    .edit()
                    .putBoolean("onAnr", true)
                    .commit()
            }
            if (prefs.getBoolean("block_oncreate", false)) {
                Log.d("ANR-Demo", "ContentProvider 开始阻塞...")
                Thread.sleep(15000) // 模拟超时
                Log.d("ANR-Demo", "ContentProvider 阻塞结束")
            }
            Log.d("ANR-Demo", "ContentProvider Start...")
            return true
        }
        
        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            Thread.sleep(15000)
            return MatrixCursor(arrayOf("_id", "data"))
        }
        
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }

    fun triggerFirstLaunch(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.killBackgroundProcesses("com.ovea_y.stabilitytool.service.AnrSimulator\$SlowContentProvider")
    }

    fun causeAppStartANR(context: Context) {
//        triggerFirstLaunch(context)
//        val uri = Uri.parse("content://com.ovea_y.stabilitytool.slowprovider/data")
//        val client = context.contentResolver.acquireContentProviderClient(uri)
//        Handler(Looper.getMainLooper()).post {
//            // 必须在主线程执行同步查询
//            context.contentResolver.query(
//                uri,
//                null,
//                null,
//                null,
//                null
//            )?.close()
//            client?.close()
//        }
        context.getSharedPreferences("anr_config", MODE_PRIVATE)
            .edit()
            .putBoolean("block_oncreate", true)
            .commit()

        scheduleRestart(context)
    }

    fun causeContentProviderANR(context: Context) {
        triggerFirstLaunch(context)
        val uri = Uri.parse("content://com.ovea_y.stabilitytool.slowprovider/data")
        val client = context.contentResolver.acquireContentProviderClient(uri)
        Handler(Looper.getMainLooper()).post {
            // 必须在主线程执行同步查询
            context.contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.close()
            client?.close()
        }
    }

    fun causeNotFocusedWindowANR(context: Context) {
        context.getSharedPreferences("anr_config", MODE_PRIVATE)
            .edit()
            .putBoolean("block_oncreate_index", true)
            .commit()

        scheduleRestart(context)
    }
    
    // 8. Service超时（前台20秒，后台200秒）
    class SlowService : Service() {
        override fun onBind(intent: Intent): IBinder {
            // 在onBind中执行耗时操作，超过20秒会导致ANR
            Thread.sleep(30000)
            return Binder()
        }
        
        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            // 在onStartCommand中执行耗时操作，超过20秒会导致ANR
            Thread.sleep(30000)
            return START_NOT_STICKY
        }
    }
    
    fun causeServiceANR(context: Context) {
        // 启动服务
        val intent = Intent(context, SlowService::class.java)
        context.startService(intent)
        
        // 或者绑定服务
        // context.bindService(intent, object : ServiceConnection {
        //     override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
        //     override fun onServiceDisconnected(name: ComponentName?) {}
        // }, Context.BIND_AUTO_CREATE)
    }
    
    // 9. 主线程等待子线程完成
    fun causeThreadWaitANR() {
        val latch = CountDownLatch(1)
        
        // 创建一个子线程，但不释放锁
        Thread {
            // 子线程永远不会释放锁
            // 或者延迟很长时间
            Thread.sleep(60000)
            latch.countDown()
        }.start()
        
        // 主线程等待子线程完成，导致ANR
        latch.await()
    }
    
    // 10. 大量同步方法调用
    @Synchronized
    fun recursiveMethod(depth: Int) {
        if (depth <= 0) return
        
        // 执行一些操作
        Thread.sleep(100)
        
        // 递归调用自身
        recursiveMethod(depth - 1)
    }
    
    fun causeSynchronizedMethodANR() {
        // 递归调用同步方法100次，总共阻塞约10秒
        recursiveMethod(100)
    }
    
    // 11. 系统调用超时
    fun causeSystemCallANR(context: Context) {
        // 尝试访问一些可能会阻塞的系统服务
        try {
            // 获取系统服务并执行可能阻塞的操作
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            // 模拟系统调用超时
            Thread.sleep(20000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
