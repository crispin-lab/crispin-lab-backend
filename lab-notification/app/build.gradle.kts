plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
    alias(libs.plugins.crispinlab.restdocs)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-notification-app")
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labCommonDomain)
    implementation(projects.labCommonPort)
    implementation(projects.labCommonPersistence)
    implementation(projects.labNotification.domain)
    implementation(projects.labUser.app)

    testImplementation(testFixtures(projects.labNotification.domain))
    testImplementation(testFixtures(projects.labCommon))
    testImplementation(testFixtures(projects.labCommonPersistence))
    testImplementation(testFixtures(projects.labUser.app))
    testImplementation(projects.labApiSupport)
    testRuntimeOnly(libs.postgresql)
}
