import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import java.io.ByteArrayOutputStream
import java.io.File


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

}
