plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labSpace.domain)
}
