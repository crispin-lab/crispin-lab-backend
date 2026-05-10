plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
    alias(libs.plugins.crispinlab.restdocs)
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labSpace.domain)
}
