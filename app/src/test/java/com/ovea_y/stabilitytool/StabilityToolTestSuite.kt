package com.ovea_y.stabilitytool

import com.ovea_y.stabilitytool.service.CpuTestServiceTest
import com.ovea_y.stabilitytool.service.DiskTestServiceTest
import com.ovea_y.stabilitytool.service.MemoryTestServiceTest
import com.ovea_y.stabilitytool.service.ResourceLeakServiceTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * 稳定性工具测试套件
 * 
 * 包含所有服务类的单元测试
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    ResourceLeakServiceTest::class,
    MemoryTestServiceTest::class,
    DiskTestServiceTest::class,
    CpuTestServiceTest::class
)
class StabilityToolTestSuite 