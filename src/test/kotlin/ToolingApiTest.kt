import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ModelBuilder
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import org.gradle.tooling.model.eclipse.EclipseProject
import org.gradle.tooling.model.idea.IdeaProject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class ToolingApiTest {

    val signalProject: String = System.getenv("HOME") + "/personal/Signal-Android"
    val wikipediaProject: String = System.getenv("HOME") + "/personal/apps-android-wikipedia"

    @Nested
    inner class FindTasks {

        @Test
        fun findAllTask_signal() {
            val toolingApi = ToolingApi()
            val allTasks = toolingApi.findAllTasks(signalProject)

            printTasks(allTasks)
        }

        @Test
        fun findAllTask_wikipedia() {
            val toolingApi = ToolingApi()
            val allTasks = toolingApi.findAllTasks(wikipediaProject)

            printTasks(allTasks)
        }


        @Test
        fun findAllTask_wikipedia_ofTypeBuild() {
            val toolingApi = ToolingApi()
            val buildTasks =
                toolingApi.findAllTasks(wikipediaProject, "build")

            printTasks(buildTasks)
        }

        private fun printTasks(tasks: Collection<GradleTask>) {
            // LaunchableGradleTask (type of GradleTask)
            tasks.forEach { task -> println("Project: ${task.project.name} --> TaskName: '${task.path}' Group: ${task.group}") }

        }
    }


    @Nested
    inner class FindProjects {
        @Test
        fun findAllProjects_signal() {
            val toolingApi = ToolingApi()
            val allProjects = toolingApi.findAllProjects(signalProject)

            printTasks(allProjects)
        }

        @Test
        fun findAllProjects_wikipedia() {
            // expects ANDROID_HOME and $SIGNAL_DIR/local.properties to be setup with sdk.dir=/Users/anugrah.singhal/android-sdk
            val toolingApi = ToolingApi()
            val allProjects = toolingApi.findAllProjects(wikipediaProject)

            printTasks(allProjects)
        }

        private fun printTasks(projects: Collection<GradleProject>) {
            // LaunchableGradleTask (type of GradleTask)
            projects.forEach { project -> println("Path: '${project.path}' --> Directory: ${project.projectDirectory.path}") }

        }
    }

    @Nested
    inner class TaskFailures {

        @Test
        fun runTask_wikipedia_version_which_fails() {
            val toolingApi = ToolingApi()
            val failureReason =
                toolingApi.runTaskAndGetFailureReason(
                    wikipediaProject,
                    "version"
                )

            System.err.println(failureReason)
        }

        @Test
        fun runTask_wikipedia_tasks() {
            val toolingApi = ToolingApi()
            val failureReason =
                toolingApi.runTaskAndGetFailureReason(
                    wikipediaProject,
                    "tasks"
                )

            System.err.println(failureReason)
        }
    }

    @Nested
    inner class ModuleDependencies {

        @Test
        fun moduleDependencies_signal_video_app() {

            val toolingApi = ToolingApi()
            toolingApi.showDependencies(System.getenv("HOME") + "/personal/Signal-Android", "");
            val signalAppConn = GradleConnector.newConnector()
                .forProjectDirectory(File(System.getenv("HOME") + "/personal/Signal-Android"))
                .connect()

            val videoAppConn = GradleConnector.newConnector()
                .forProjectDirectory(File(System.getenv("HOME") + "/personal/Signal-Android/video/app"))
                .connect()

            val ideaProjectModel = videoAppConn.getModel(IdeaProject::class.java)
            val eclipseProjectModel: EclipseProject = videoAppConn.getModel(EclipseProject::class.java)
        }
    }

    @Nested
    inner class FindArtifactForClassName {
        @Test
        fun findArtifactForClassName_signal() {
            // Connect to the Gradle project
            ToolingApi().findArtifactForClassName(
                System.getenv("HOME") + "/personal/Signal-Android",
                "androidx.compose.ui.Modifier"
            )
        }
    }

    @Nested
    inner class FindPluginsForModule {
        @Test
        fun findPluginsForModule_signal() {
            // Connect to the Gradle project
            val findPluginsAppliedToModule = ToolingApi().findPluginsAppliedToModule(
                System.getenv("HOME") + "/personal/Signal-Android/video/app",
                "video-app"
            )

            println(findPluginsAppliedToModule)
        }
    }

    @Test
    fun myExp() {
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(signalProject))
            .connect()

        val customModelBuilder = connection.model(ConfigurationDependenciesModel::class.java)
        val withArguments: ModelBuilder<ConfigurationDependenciesModel> =
            customModelBuilder.withArguments("--init-script", ToolingApi().copyInitScript().absolutePath)
//        customModelBuilder.withArguments("-Dorg.gradle.debug=true")
//        customModelBuilder.withArguments("--no-daemon")

        val customModel: ConfigurationDependenciesModel = withArguments.get()

//        customModel.pluginJarPaths().forEach(System.out::println)

        println()
        println()

//        ToolingApi().getClassNamesFromJarFile(File("/Users/anugrah.singhal/.gradle/caches/modules-2/files-2.1/com.squareup.wire/wire-gradle-plugin/4.4.3/9ecfa0cf7b2a59d816909ff9edf1fd871f276ec4/wire-gradle-plugin-4.4.3.jar"))
//            .forEach(System.out::println)

        println(customModel.debug())
    }
}