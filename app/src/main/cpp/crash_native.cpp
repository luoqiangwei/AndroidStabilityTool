#include <jni.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>

#define TAG "CrashNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 递归函数声明
void recursiveFunction(int depth);

extern "C" {

// 触发SIGSEGV（段错误）
JNIEXPORT void JNICALL
Java_com_ovea_1y_stabilitytool_service_CrashNative_causeSIGSEGV(JNIEnv *env, jobject thiz) {
    LOGI("Causing SIGSEGV...");
    // 访问空指针，触发段错误
    int *ptr = NULL;
    *ptr = 42;  // 这里会崩溃
}

// 触发SIGABRT（异常终止）
JNIEXPORT void JNICALL
Java_com_ovea_1y_stabilitytool_service_CrashNative_causeSIGABRT(JNIEnv *env, jobject thiz) {
    LOGI("Causing SIGABRT...");
    // 调用abort()函数，触发SIGABRT信号
    abort();
}

// 触发非法指令
JNIEXPORT void JNICALL
Java_com_ovea_1y_stabilitytool_service_CrashNative_causeIllegalInstruction(JNIEnv *env, jobject thiz) {
    LOGI("Causing Illegal Instruction...");
    // 使用SIGILL信号
    raise(SIGILL);
}

// 触发除零错误
JNIEXPORT void JNICALL
Java_com_ovea_1y_stabilitytool_service_CrashNative_causeDivideByZero(JNIEnv *env, jobject thiz) {
    LOGI("Causing Divide by Zero...");
    // 除以零，触发SIGFPE信号
    volatile int zero = 0;
    volatile int result = 1 / zero;
}

// 触发栈溢出
JNIEXPORT void JNICALL
Java_com_ovea_1y_stabilitytool_service_CrashNative_causeStackOverflow(JNIEnv *env, jobject thiz) {
    LOGI("Causing Stack Overflow...");
    // 调用递归函数
    recursiveFunction(0);
}

// 触发内存访问越界
JNIEXPORT void JNICALL
Java_com_ovea_1y_stabilitytool_service_CrashNative_causeOutOfBounds(JNIEnv *env, jobject thiz) {
    LOGI("Causing Out of Bounds Access...");
    // 分配一个小数组，然后访问越界
    int array[5] = {1, 2, 3, 4, 5};
    // 访问越界
    for (int i = 0; i < 100; i++) {
        array[i] = i;  // 当i>=5时会越界
    }
}

} // extern "C"

// 递归函数实现
void recursiveFunction(int depth) {
    // 在栈上分配一些内存，加速栈溢出
    char buffer[1024];
    // 防止编译器优化掉buffer
    buffer[0] = depth & 0xFF;
    // 递归调用
    recursiveFunction(depth + 1);
} 