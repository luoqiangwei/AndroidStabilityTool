plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ovea_y.stabilitytool"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ovea_y.stabilitytool"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // 指定NDK版本
        ndkVersion = "25.2.9519653"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // 配置CMake构建
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    // 配置测试选项
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    
    // 测试依赖
    testImplementation(libs.junit)
    
    // Mockito依赖 - 完整版本
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito:mockito-inline:5.2.0") // 用于模拟final类和静态方法
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1") // Kotlin友好的Mockito API
    
    // Robolectric依赖 - 修正版本
    testImplementation("org.robolectric:robolectric:4.11.1") // 包含所有必要的依赖
    testImplementation("org.robolectric:shadows-framework:4.11.1") // Shadows API
    
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // 协程测试
    testImplementation("io.mockk:mockk:1.13.9") // Kotlin友好的模拟库
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// 自定义任务：运行所有单元测试
tasks.register("runAllTests") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "运行所有单元测试"
    
    doLast {
        println("所有单元测试已完成")
    }
}

// 自定义任务：运行稳定性工具测试套件
tasks.register<JavaExec>("runStabilityToolTests") {
    group = "verification"
    description = "运行稳定性工具测试套件"
    
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.ovea_y.stabilitytool.TestRunner")
    
    // 设置JVM参数
    jvmArgs = listOf("-Xmx512m")
}