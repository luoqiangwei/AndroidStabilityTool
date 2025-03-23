package com.ovea_y.stabilitytool.service

import android.content.Context
import android.os.Process
import java.io.File
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * 崩溃类型
 * - 1. Java崩溃
 *   - 1.1 NullPointerException
 *   - 1.2 ArrayIndexOutOfBoundsException
 *   - 1.3 ClassCastException
 *   - 1.4 OutOfMemoryError
 *   - 1.5 StackOverflowError
 * - 2. Native崩溃
 *   - 2.1 SIGSEGV (段错误)
 *   - 2.2 SIGABRT (异常终止)
 * - 3. 应用主动退出
 */
class CrashService {
    // 本地崩溃实现
    private val crashNative = CrashNative()

    // 1.1 空指针异常
    fun causeNullPointerException() {
        val nullString: String? = null
        nullString!!.length // 强制解引用空值
    }

    // 1.2 数组越界异常
    fun causeArrayIndexOutOfBoundsException() {
        val array = IntArray(5)
        array[10] = 100 // 访问超出数组范围的索引
    }

    // 1.3 类型转换异常
    fun causeClassCastException() {
        val obj: Any = "Hello"
        val number = obj as Int // 尝试将String转换为Int
    }

    // 1.4 内存溢出错误
    fun causeOutOfMemoryError() {
        val list = ArrayList<ByteArray>()
        while (true) {
            list.add(ByteArray(10 * 1024 * 1024)) // 不断分配大内存块
        }
    }

    // 1.5 栈溢出错误
    fun causeStackOverflowError() {
        fun recursiveFunction() {
            recursiveFunction() // 无限递归调用
        }
        recursiveFunction()
    }

    // 2.1 SIGSEGV (段错误) - 使用JNI实现
    fun causeSIGSEGV() {
        crashNative.causeSIGSEGV()
    }

    // 2.2 SIGABRT (异常终止) - 使用JNI实现
    fun causeSIGABRT() {
        crashNative.causeSIGABRT()
    }

    // 2.3 非法指令 - 使用JNI实现
    fun causeIllegalInstruction() {
        crashNative.causeIllegalInstruction()
    }

    // 2.4 除零错误 - 使用JNI实现
    fun causeDivideByZero() {
        crashNative.causeDivideByZero()
    }

    // 2.5 Native栈溢出 - 使用JNI实现
    fun causeNativeStackOverflow() {
        crashNative.causeStackOverflow()
    }

    // 2.6 内存访问越界 - 使用JNI实现
    fun causeOutOfBounds() {
        crashNative.causeOutOfBounds()
    }

    // 3. 应用主动退出
    fun causeAppExit() {
        System.exit(1)
    }

    // 模拟文件系统崩溃
    fun causeFileSystemCrash(context: Context) {
        thread {
            try {
                // 尝试在应用数据目录创建大量文件直到文件系统崩溃
                val dir = context.filesDir
                var i = 0
                while (true) {
                    val file = File(dir, "crash_test_$i")
                    file.createNewFile()
                    val output = file.outputStream()
                    // 写入大量数据
                    val data = ByteArray(10 * 1024 * 1024) // 10MB
                    output.write(data)
                    output.flush()
                    output.close()
                    i++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
