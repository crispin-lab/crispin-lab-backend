import com.crispinlab.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class KopringExposedConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("implementation", libs.findLibrary("exposed.spring.boot.starter").get())
                add("runtimeOnly", libs.findLibrary("h2.database").get())
            }
        }
    }
}
