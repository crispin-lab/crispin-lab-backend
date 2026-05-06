package com.crispinlab.common.id

import com.crispinlab.Snowflake

class SnowflakeIdGenerator private constructor(
    private val snowflake: Snowflake
) : IdGenerator {
    override fun next(): Long = snowflake.nextId()

    companion object {
        fun create(): SnowflakeIdGenerator = SnowflakeIdGenerator(Snowflake.create())

        fun ofNode(nodeId: Long): SnowflakeIdGenerator =
            SnowflakeIdGenerator(Snowflake.create(nodeId))
    }
}
