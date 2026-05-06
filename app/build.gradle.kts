plugins {
    alias(libs.plugins.crispinlab.kopring.web)
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labSpace.app)
}
