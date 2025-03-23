package com.ovea_y.stabilitytool.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

/**
 * 用于任务调度器泄漏测试的空JobService
 * 这个服务不会实际执行任何操作，仅用于创建有效的JobInfo
 */
class DummyJobService : JobService() {
    private val TAG = "DummyJobService"
    
    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "任务开始执行: ${params?.jobId}")
        // 返回false表示任务已完成
        return false
    }
    
    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "任务被停止: ${params?.jobId}")
        // 返回false表示不需要重新调度
        return false
    }
} 