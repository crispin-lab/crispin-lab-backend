import com.crispinlab.applySpringBootBom
import com.crispinlab.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class KopringLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins("crispinlab.jvm")
            applySpringBootBom(configuration = "api")
        }
    }
}
