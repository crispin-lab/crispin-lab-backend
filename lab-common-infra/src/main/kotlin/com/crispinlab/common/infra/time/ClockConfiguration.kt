package com.crispinlab.common.infra.time

import com.crispinlab.common.time.Clock
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ClockConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock::class)
    fun clock(): Clock = SystemClock()
}
