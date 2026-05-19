plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
    alias(libs.plugins.crispinlab.restdocs)
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

    testImplementation(testFixtures(projects.labUser.domain))
    testImplementation(testFixtures(projects.labCommonPersistence))
    testImplementation(projects.labApiSupport)
    testRuntimeOnly(libs.postgresql)
}
