import com.crispinlab.libs
import com.crispinlab.plugins
import com.crispinlab.setServers
import com.epages.restdocs.apispec.gradle.OpenApi3Extension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class RestdocsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins("com.epages.restdocs-api-spec")

            dependencies {
                add("testImplementation", libs.findLibrary("spring.restdocs.mockmvc").get())
                add("testImplementation", libs.findLibrary("restdocs.api.spec.mockmvc").get())
                add("testImplementation", libs.findLibrary("spring.boot.restdocs").get())
            }

            extensions.configure<OpenApi3Extension>("openapi3") {
                setServers("http://localhost:8080")
                title = "Crispin Lab API"
                format = "json"
            }
        }
    }
}
