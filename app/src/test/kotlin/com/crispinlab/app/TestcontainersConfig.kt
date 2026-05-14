package com.crispinlab.app

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

/**
 * app 모듈의 ApplicationTest 부팅 스모크용 datasource wiring.
 *
 * lab-space/app 의 PostgresTestContext 가 별개의 컨테이너 인스턴스를 가진다 — 모듈
 * 의존 방향상 재사용할 수 없어 같은 빌드에서 컨테이너 두 개가 동시 기동될 수 있다.
 * 의도된 트레이드오프이며, reusable 컨테이너 패턴 도입 시 별도 티켓에서 단일화한다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:16")
}
