plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
    alias(libs.plugins.crispinlab.restdocs)
    `java-test-fixtures`
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-user-app")
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labCommonDomain)
    implementation(projects.labCommonPort)
    implementation(projects.labCommonPersistence)
    implementation(projects.labUser.domain)

    implementation(libs.spring.security.crypto)
    implementation(libs.spring.boot.starter.data.redis)

    testFixturesImplementation(platform(libs.spring.boot.bom))
    testFixturesApi(projects.labUser.domain)
    testFixturesApi(libs.spring.test)
    testFixturesApi(libs.spring.webmvc)
    testFixturesApi(libs.jakarta.servlet.api)
    testFixturesImplementation(projects.labCommon)

    testImplementation(testFixtures(projects.labUser.domain))
    testImplementation(testFixtures(projects.labCommonPersistence))
    testImplementation(projects.labApiSupport)
    testImplementation(libs.testcontainers)
    testRuntimeOnly(libs.postgresql)
}
