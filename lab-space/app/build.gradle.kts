plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
    alias(libs.plugins.crispinlab.restdocs)
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labSpace.domain)

    testImplementation(testFixtures(projects.labSpace.domain))
    testImplementation(projects.labApiSupport)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    // Flyway 는 PostgresTestContext schema bootstrap 전용 — 운영 실행은 app 모듈 책임.
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testRuntimeOnly(libs.postgresql)
}
