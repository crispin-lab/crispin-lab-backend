package com.crispinlab.common.infra.jackson

import com.crispinlab.common.domain.EntityId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    afterName = ["org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration"]
)
@ConditionalOnClass(ObjectMapper::class, SimpleModule::class)
class EntityIdJacksonConfiguration {
    @Bean
    fun entityIdJacksonModule(): SimpleModule =
        SimpleModule().apply {
            addSerializer(EntityId::class.java, EntityIdSerializer)
        }
}
