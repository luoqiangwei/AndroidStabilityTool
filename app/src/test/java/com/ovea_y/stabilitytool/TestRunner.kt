package com.ovea_y.stabilitytool

import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.notification.Failure

/**
 * 测试运行器
 * 
 * 用于在命令行中运行测试
 */
object TestRunner {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("开始运行稳定性工具测试...")
        
        val result: Result = JUnitCore.runClasses(StabilityToolTestSuite::class.java)
        
        // 打印测试结果
        println("测试完成！")
        println("运行测试数: ${result.runCount}")
        println("失败测试数: ${result.failureCount}")
        println("忽略测试数: ${result.ignoreCount}")
        println("运行时间: ${result.runTime}ms")
        
        // 打印失败的测试
        if (result.failureCount > 0) {
            println("\n失败的测试:")
            for (failure: Failure in result.failures) {
                println("${failure.testHeader}: ${failure.message}")
                println("${failure.trace}\n")
            }
        }
        
        // 退出码
        System.exit(if (result.wasSuccessful()) 0 else 1)
    }
} 