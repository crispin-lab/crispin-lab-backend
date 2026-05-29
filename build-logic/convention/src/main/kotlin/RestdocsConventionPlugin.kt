import com.crispinlab.libs
import com.crispinlab.plugins
import com.crispinlab.setServers
import com.epages.restdocs.apispec.gradle.OpenApi3Extension
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register

@Suppress("unused")
class RestdocsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins("com.epages.restdocs-api-spec")

            dependencies {
                add("testImplementation", libs.findLibrary("spring.restdocs.mockmvc").get())
                add("testImplementation", libs.findLibrary("restdocs.api.spec.mockmvc").get())
            }

            extensions.configure<OpenApi3Extension>("openapi3") {
                setServers("http://localhost:8080")
                title = "Crispin Lab API"
                format = "json"
            }

            val specFile = layout.buildDirectory.file("api-spec/openapi3.json")

            val verifyTask =
                tasks.register("verifyOpenApiSchemaNames") {
                    group = "verification"
                    description =
                        "openapi3.json 의 components.schemas 이름이 도메인 의미를 갖는지 검증"
                    outputs.upToDateWhen { false }
                    doLast {
                        val file = specFile.get().asFile
                        if (!file.exists()) {
                            throw GradleException(
                                "openapi3.json 이 생성되지 않았습니다: ${file.path}"
                            )
                        }
                        val root = JsonSlurper().parse(file) as? Map<*, *>
                            ?: throw GradleException("openapi3.json 형식이 올바르지 않습니다.")
                        val components = root["components"] as? Map<*, *>
                        val schemas = components?.get("schemas") as? Map<*, *>
                        val keys = schemas?.keys.orEmpty().filterIsInstance<String>()
                        val invalid = keys.filterNot { schemaNamePattern.matches(it) }
                        if (invalid.isNotEmpty()) {
                            throw GradleException(
                                buildString {
                                    appendLine(
                                        "openapi3.json 의 components.schemas 에 도메인 이름이 아닌 항목이 있습니다."
                                    )
                                    appendLine(
                                        "ControllerDescribeSpec.document() 의 schema 인자로 명시하세요."
                                    )
                                    invalid.forEach { appendLine("  - $it") }
                                }
                            )
                        }
                    }
                }

            tasks
                .matching { it.name == "openapi3" }
                .configureEach { finalizedBy(verifyTask) }
        }
    }

    companion object {
        private val schemaNamePattern = Regex("^(?:[A-Z][a-z0-9]+)+$")
    }
}
