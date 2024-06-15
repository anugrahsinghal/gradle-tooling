import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import org.gradle.tooling.model.idea.*
import java.io.*
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

class ToolingApi {

    fun findAllTasks(projectLocation: String, taskGroup: String? = null): Collection<GradleTask> {
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val gradleProject = connection.use { it.getModel(GradleProject::class.java) }

        val childProjectTasks = gradleProject.children.flatMap { it.tasks }

        val gradleTasks = gradleProject.tasks + childProjectTasks

        if (taskGroup != null) {
            return gradleTasks.filter { it.group == taskGroup }
        }

        return gradleTasks
    }

    fun findAllProjects(projectLocation: String): Collection<GradleProject> {
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val gradleProject = connection.use { it.getModel(GradleProject::class.java) }

        val gradleProjects = gradleProject.children + gradleProject

        return gradleProjects
    }

    fun runTaskAndGetFailureReason(projectLocation: String, taskName: String): String {
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val errorStream = ByteArrayOutputStream()
        val buildTasks = connection.newBuild().forTasks(taskName).setStandardError(errorStream)

        try {
            buildTasks.run()
        } catch (e: Exception) {
            // just return error stream for now
            return errorStream.toString("UTF-8")
        }

        return "no-errors"
    }

    fun findPluginsAppliedToModule(projectLocation: String, moduleName: String): Collection<String>? {
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val customModelBuilder = connection.model(ConfigurationDependenciesModel::class.java)
        customModelBuilder.withArguments("--init-script", copyInitScript().absolutePath)

        // Fetch the custom model
        val customModel: ConfigurationDependenciesModel = customModelBuilder.get()

        val projectPluginMap = customModel.projectPluginMap()

        // todo: check module exists within project before trying to get it's data
        return projectPluginMap[moduleName]?.toSet()

    }

    private fun copyInitScript(): File {
        val init = Files.createTempFile("init", ".gradle")
        val sb = StringBuilder()
        val pluginJar = lookupJar(ConfigurationDependenciesModel::class.java)
        val modelJar = lookupJar(DefaultDependenciesModel::class.java)
        BufferedReader(
            InputStreamReader(this::class.java.getResourceAsStream("/init.gradle"))
        ).use { reader ->
            reader.lines()
                .forEach { line: String ->
                    var repl = line
                        .replace("%%PLUGIN_JAR%%", pluginJar.absolutePath)
                        .replace("%%MODEL_JAR%%", modelJar.absolutePath)
                        .replace("%%CUSTOM_JAR%%", modelJar.absolutePath)
                    // fix paths if we're on Windows
                    if (File.separatorChar == '\\') {
                        repl = repl.replace('\\', '/')
                    }
                    sb.append(repl)
                        .append("\n")
                }
        }
        Files.copy(
            ByteArrayInputStream(sb.toString().toByteArray(Charset.defaultCharset())),
            init,
            StandardCopyOption.REPLACE_EXISTING
        )
        return init.toFile()
    }

    @Throws(URISyntaxException::class)
    private fun lookupJar(beaconClass: Class<*>): File {
        val codeSource = beaconClass.protectionDomain.codeSource
        return File(codeSource.location.toURI())
    }


}
