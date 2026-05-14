plugins {
    alias(libs.plugins.crispinlab.kopring.service)
    alias(libs.plugins.crispinlab.snowflake)
}

dependencies {
    implementation(projects.labCommon)
    implementation(libs.spring.tx)
    compileOnly(libs.spring.web)
    compileOnly(libs.jakarta.servlet.api)

    testImplementation(libs.spring.jdbc)
    testImplementation(libs.spring.webmvc)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.postgresql)
}
