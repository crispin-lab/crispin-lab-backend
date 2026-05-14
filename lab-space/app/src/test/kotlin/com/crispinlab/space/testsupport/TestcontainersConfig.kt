package com.crispinlab.space.testsupport

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

/**
 * lab-space/app 의 @SpringBootTest 가 사용하는 datasource wiring.
 *
 * 본 빈은 `PostgresTestContext.container` 를 그대로 노출해 repository spec 들과 같은
 * 컨테이너를 공유한다. app 모듈은 별도의 동일 패턴 TestcontainersConfig 를 자체적으로
 * 두며, 두 모듈은 모듈 의존 방향상 컨테이너 인스턴스를 공유할 수 없다 — 동일 빌드에서
 * 두 모듈 테스트가 같은 JVM 으로 묶여 돌면 컨테이너 두 개가 동시 기동될 수 있다.
 * 의도된 트레이드오프이며, reusable 컨테이너 패턴 도입 시 별도 티켓에서 단일화한다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> = PostgresTestContext.container
}
