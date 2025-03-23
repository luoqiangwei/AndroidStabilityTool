package com.ovea_y.stabilitytool.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * 从Context获取Activity实例
 * 
 * @return Activity实例
 * @throws IllegalStateException 如果无法获取Activity实例
 */
fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    throw IllegalStateException("无法从Context获取Activity实例")
} 