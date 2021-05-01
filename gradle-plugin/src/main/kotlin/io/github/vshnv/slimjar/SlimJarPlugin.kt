package io.github.vshnv.slimjar

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.vshnv.slimjar.func.createConfig
import io.github.vshnv.slimjar.task.SlimJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

const val SLIM_CONFIGURATION_NAME = "slim"
const val SLIM_API_CONFIGURATION_NAME = "slimApi"
const val SLIM_JAR_TASK_NAME = "slimJar"
private const val RESOURCES_TASK = "processResources"

class SlimJarPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        // Applies Java if not present, since it's required for the compileOnly configuration
        plugins.apply(JavaPlugin::class.java)

        val slimConfig = createConfig(SLIM_CONFIGURATION_NAME, JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
        //val slimApiConfig = createConfig(SLIM_API_CONFIGURATION_NAME, JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME)

        val slimJar = tasks.create("slimJar", SlimJar::class.java, slimConfig)

        // Checks if shadow is present
        if (tasks.findByName("shadowJar") == null) {
            // TODO Create the task for relocating without shadow
            //tasks.withType(Jar::class.java).first().finalizedBy(slimJar)
        } else {
            // Hooks into shadow to inject relocations
            val shadowTask = tasks.withType(ShadowJar::class.java).firstOrNull() ?: return
            shadowTask.doFirst { _ ->
                slimJar.relocations().forEach { rule ->
                    shadowTask.relocate(
                        rule.originalPackagePattern,
                        rule.relocatedPackagePattern
                    ) {
                        rule.inclusions.forEach(it::include)
                        rule.exclusions.forEach(it::exclude)
                    }
                }
            }

        }

        // Runs the task once resources are being processed to save the json file
        tasks.findByName(RESOURCES_TASK)?.finalizedBy(slimJar)

    }

}