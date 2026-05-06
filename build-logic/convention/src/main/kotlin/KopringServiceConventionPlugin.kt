import com.crispinlab.libs
import com.crispinlab.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class KopringServiceConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins("crispinlab.kopring.base")

            dependencies {
                add("implementation", libs.findLibrary("spring.boot.starter").get())
            }
        }
    }
}
