import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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

}