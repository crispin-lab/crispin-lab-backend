package com.crispinlab.common.infra.jackson

import com.crispinlab.common.domain.StringValue
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

object StringValueSerializer : JsonSerializer<StringValue>() {
    override fun serialize(
        value: StringValue,
        generator: JsonGenerator,
        provider: SerializerProvider
    ) {
        generator.writeString(value.value)
    }
}
