package com.crispinlab.common.persistence

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

// 같은 빌드 JVM 의 모듈들이 [PostgresTestContext.container] 인스턴스를 공유한다.
// app 모듈은 자체 TestcontainersConfig (별도 컨테이너 인스턴스) 를 둘 수 있어 본 헬퍼와 공존 가능.
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> = PostgresTestContext.container
}
