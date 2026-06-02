plugins {
    alias(libs.plugins.crispinlab.jvm)
    alias(libs.plugins.crispinlab.snowflake)
    `java-test-fixtures`
}

dependencies {
    api(projects.labCommonPort)
}
