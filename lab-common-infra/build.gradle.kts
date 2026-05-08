plugins {
    alias(libs.plugins.crispinlab.kopring.service)
}

dependencies {
    implementation(projects.labCommon)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.jdbc)
    testRuntimeOnly(libs.h2.database)
}
