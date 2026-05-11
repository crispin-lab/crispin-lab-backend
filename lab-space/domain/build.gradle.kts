plugins {
    alias(libs.plugins.crispinlab.jvm)
    `java-test-fixtures`
}

dependencies {
    implementation(projects.labCommon)

    testFixturesImplementation(projects.labCommon)
}
