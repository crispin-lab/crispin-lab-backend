package com.crispinlab.common.infra.id

import com.crispinlab.Snowflake
import com.crispinlab.common.id.IdGenerator
import java.time.Instant

class SnowflakeIdGenerator(
    nodeId: Long = DEFAULT_NODE_ID,
    customEpoch: Long = DEFAULT_EPOCH_MILLIS
) : IdGenerator {
    private val snowflake: Snowflake = Snowflake.create(nodeId, customEpoch)

    override fun next(): Long = snowflake.nextId()

    companion object {
        private const val DEFAULT_NODE_ID: Long = 0L
        private val DEFAULT_EPOCH_MILLIS: Long =
            Instant.parse("2025-01-01T00:00:00Z").toEpochMilli()
    }
}
