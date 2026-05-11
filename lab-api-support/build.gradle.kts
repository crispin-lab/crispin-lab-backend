plugins {
    alias(libs.plugins.crispinlab.kopring.web)
}

dependencies {
    api(projects.labCommon)

    api(libs.spring.restdocs.mockmvc)
    api(libs.restdocs.api.spec.mockmvc)
    api(libs.kotest.runner.junit5)
    api(libs.springmockk)
}
