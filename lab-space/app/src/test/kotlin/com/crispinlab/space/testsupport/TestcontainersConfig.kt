package com.crispinlab.space.testsupport

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

// app 모듈도 동일 패턴의 자체 TestcontainersConfig 를 둔다 — 모듈 의존 방향상
// 컨테이너 인스턴스를 공유할 수 없어, 같은 빌드 JVM 에서 두 컨테이너가 동시 기동될 수 있다.
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> = PostgresTestContext.container
}
