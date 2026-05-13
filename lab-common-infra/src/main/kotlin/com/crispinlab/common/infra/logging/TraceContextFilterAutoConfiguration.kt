package com.crispinlab.common.infra.logging

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered

@AutoConfiguration
class TraceContextFilterAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(TraceIdGenerator::class)
    fun traceIdGenerator(): TraceIdGenerator = TraceIdGenerator()

    @Bean
    fun traceContextFilterRegistration(
        generator: TraceIdGenerator
    ): FilterRegistrationBean<TraceContextFilter> =
        FilterRegistrationBean(TraceContextFilter(generator)).apply {
            order = Ordered.HIGHEST_PRECEDENCE + 1
            urlPatterns = listOf("/*")
        }
}
