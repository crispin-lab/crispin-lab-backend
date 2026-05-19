package com.crispinlab.space

import com.crispinlab.common.persistence.TestcontainersConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackageClasses = [SpaceModule::class])
@Import(TestcontainersConfig::class)
class TestSpaceApplication
