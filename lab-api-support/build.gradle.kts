plugins {
    alias(libs.plugins.crispinlab.kopring.library)
}

dependencies {
    api(projects.labCommonDomain)
    api(projects.labCommonPort)
    api(projects.labCommonInfra)

    api(libs.spring.restdocs.mockmvc)
    api(libs.restdocs.api.spec.mockmvc)
    api(libs.spring.webmvc)
    api(libs.spring.test)
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)
    api(libs.kotest.runner.junit5)
    api(libs.mockk)
}
