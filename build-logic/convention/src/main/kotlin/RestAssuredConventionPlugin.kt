import com.crispinlab.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class RestAssuredConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("testImplementation", libs.findLibrary("rest.assured").get())
                add("testImplementation", libs.findLibrary("rest.assured.kotlin.extensions").get())
            }
        }
    }
}
