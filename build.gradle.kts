// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// 添加NDK下载配置
tasks.register("downloadNDK") {
    doLast {
        println("Downloading NDK...")
    }
}

// 在构建前下载NDK
tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn("downloadNDK")
    }
}