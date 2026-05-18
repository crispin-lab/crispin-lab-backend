plugins {
    alias(libs.plugins.crispinlab.jvm)
    alias(libs.plugins.crispinlab.snowflake)
}

dependencies {
    api(projects.labCommonPort)
}
