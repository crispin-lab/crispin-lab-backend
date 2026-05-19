package com.crispinlab.common.infra.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.RestControllerAdvice

@AutoConfiguration
@ConditionalOnClass(RestControllerAdvice::class)
class GlobalExceptionHandlerConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()
}
