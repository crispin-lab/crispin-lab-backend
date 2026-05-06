plugins {
    alias(libs.plugins.crispinlab.jvm)
    alias(libs.plugins.crispinlab.snowflake)
}

dependencies {
    implementation(projects.labCommon)
}
