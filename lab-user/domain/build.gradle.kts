plugins {
    alias(libs.plugins.crispinlab.jvm)
    `java-test-fixtures`
}

dependencies {
    api(projects.labCommon)
}
