package com.crispinlab.user.testsupport

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

object RedisTestContext {
    private const val REDIS_PORT: Int = 6379

    val container: GenericContainer<*> =
        GenericContainer("redis:7-alpine")
            .withExposedPorts(REDIS_PORT)
            .waitingFor(Wait.forListeningPort())
            .apply { start() }

    private val connectionFactory: LettuceConnectionFactory =
        LettuceConnectionFactory(container.host, container.getMappedPort(REDIS_PORT))
            .apply { afterPropertiesSet() }

    val redisTemplate: StringRedisTemplate =
        StringRedisTemplate(connectionFactory).apply { afterPropertiesSet() }

    fun flushAll() {
        connectionFactory.connection.use { it.serverCommands().flushAll() }
    }
}
