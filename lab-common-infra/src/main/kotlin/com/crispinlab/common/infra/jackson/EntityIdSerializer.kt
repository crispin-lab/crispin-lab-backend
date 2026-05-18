package com.crispinlab.common.infra.jackson

import com.crispinlab.common.domain.EntityId
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

object EntityIdSerializer : JsonSerializer<EntityId>() {
    override fun serialize(
        value: EntityId,
        generator: JsonGenerator,
        provider: SerializerProvider
    ) {
        generator.writeString(value.value.toString())
    }
}
