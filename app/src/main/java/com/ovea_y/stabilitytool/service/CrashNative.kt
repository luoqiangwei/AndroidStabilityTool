package com.ovea_y.stabilitytool.service

/**
 * 本地崩溃测试类
 * 通过JNI实现各种Native崩溃
 */
class CrashNative {
    companion object {
        // 加载本地库
        init {
            try {
                System.loadLibrary("crash_native")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
    }

    // 触发SIGSEGV（段错误）
    external fun causeSIGSEGV()

    // 触发SIGABRT（异常终止）
    external fun causeSIGABRT()

    // 触发非法指令
    external fun causeIllegalInstruction()

    // 触发除零错误
    external fun causeDivideByZero()

    // 触发栈溢出
    external fun causeStackOverflow()

    // 触发内存访问越界
    external fun causeOutOfBounds()
} 