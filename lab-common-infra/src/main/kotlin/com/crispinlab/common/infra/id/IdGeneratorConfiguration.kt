package com.crispinlab.common.infra.id

import com.crispinlab.common.id.IdGenerator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class IdGeneratorConfiguration {
    @Bean
    @ConditionalOnMissingBean(IdGenerator::class)
    fun idGenerator(): IdGenerator = SnowflakeIdGenerator()
}
