package com.crispinlab.common.infra.id

import com.crispinlab.common.id.IdGenerator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class IdGeneratorConfiguration {
    @Bean
    @ConditionalOnMissingBean(IdGenerator::class)
    fun idGenerator(): IdGenerator = SnowflakeIdGenerator()
}
