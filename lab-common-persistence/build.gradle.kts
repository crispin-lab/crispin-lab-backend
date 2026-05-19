plugins {
    alias(libs.plugins.crispinlab.kopring.library)
    alias(libs.plugins.crispinlab.kopring.exposed)
    `java-test-fixtures`
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-common-persistence")
}

dependencies {
    api(projects.labCommonDomain)

    // testFixtures 는 main 의 implementation 을 inherit 안 받음 — consumer 가 직접 import 하는 타입은 api 로 노출.
    testFixturesApi(libs.exposed.spring.boot.starter)
    testFixturesApi(libs.exposed.java.time)
    testFixturesApi(libs.spring.boot.test)
    testFixturesApi(libs.spring.boot.test.autoconfigure)
    testFixturesApi(libs.spring.boot.testcontainers)
    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesApi(libs.flyway.core)
    testFixturesApi(libs.flyway.database.postgresql)
}
