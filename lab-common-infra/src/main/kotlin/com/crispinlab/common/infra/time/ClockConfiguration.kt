package com.crispinlab.common.infra.time

import com.crispinlab.common.time.Clock
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class ClockConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock::class)
    fun clock(): Clock = SystemClock()
}
