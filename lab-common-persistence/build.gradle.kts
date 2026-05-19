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

    // testFixtures source set 은 main 의 implementation 의존을 inherit 받지 않으므로
    // PostgresTestContext / TestcontainersConfig 가 쓰는 의존을 명시적으로 다시 받는다.
    // consumer (다른 모듈) 의 test 코드가 PostgreSQLContainer · TestConfiguration 등 타입을
    // 직접 import 하므로 testFixturesApi 로 노출한다.
    testFixturesApi(libs.exposed.spring.boot.starter)
    testFixturesApi(libs.exposed.java.time)
    testFixturesApi(libs.spring.boot.test)
    testFixturesApi(libs.spring.boot.test.autoconfigure)
    testFixturesApi(libs.spring.boot.testcontainers)
    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesApi(libs.flyway.core)
    testFixturesApi(libs.flyway.database.postgresql)
}
