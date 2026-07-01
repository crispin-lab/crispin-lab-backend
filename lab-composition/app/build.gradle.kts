plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.restdocs)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-composition-app")
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labCommonDomain)
    implementation(projects.labCommonPort)
    implementation(projects.labUser.domain)
    implementation(projects.labUser.app)
    implementation(projects.labSpace.domain)
    implementation(projects.labSpace.app)
    implementation(projects.labNotification.domain)
    implementation(projects.labNotification.app)

    testImplementation(testFixtures(projects.labCommon))
    testImplementation(testFixtures(projects.labSpace.domain))
    testImplementation(testFixtures(projects.labUser.app))
    testImplementation(projects.labApiSupport)
}
