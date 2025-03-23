package com.ovea_y.stabilitytool.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限请求辅助类
 */
object PermissionUtils {
    
    // 所有需要运行时请求的权限
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
    )
    
    // 权限请求码
    const val PERMISSION_REQUEST_CODE = 100
    
    /**
     * 检查是否有所有必要的权限
     */
    fun hasAllPermissions(context: Context): Boolean {
        // Android 6.0以下不需要运行时权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        
        for (permission in PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    
    /**
     * 请求所有必要的权限
     */
    fun requestAllPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
    
    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
                return true
            }
        }
        return false
    }
} 