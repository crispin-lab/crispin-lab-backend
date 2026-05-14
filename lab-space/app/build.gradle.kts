plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
    alias(libs.plugins.crispinlab.restdocs)
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labSpace.domain)

    // Flyway 는 PostgresTestContext 의 schema bootstrap 용 (test scope 전용).
    // 운영 실행은 app 모듈이 책임지며, 본 모듈은 SQL 만 classpath 로 제공.
    testImplementation(testFixtures(projects.labSpace.domain))
    testImplementation(projects.labApiSupport)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testRuntimeOnly(libs.postgresql)
}
