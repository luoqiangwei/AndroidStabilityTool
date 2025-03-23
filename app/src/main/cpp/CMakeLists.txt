cmake_minimum_required(VERSION 3.4.1)

# 设置库名称
project(crash_native)

# 添加日志库
find_library(log-lib log)

# 添加源文件
add_library(
        crash_native
        SHARED
        crash_native.cpp
)

# 链接库
target_link_libraries(
        crash_native
        ${log-lib}
) 