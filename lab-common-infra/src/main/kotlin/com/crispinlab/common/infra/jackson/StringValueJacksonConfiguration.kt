package com.crispinlab.common.infra.jackson

import com.crispinlab.common.domain.StringValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    afterName = ["org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration"]
)
@ConditionalOnClass(ObjectMapper::class, SimpleModule::class)
class StringValueJacksonConfiguration {
    @Bean
    fun stringValueJacksonModule(): SimpleModule =
        SimpleModule().apply {
            addSerializer(StringValue::class.java, StringValueSerializer)
        }
}
