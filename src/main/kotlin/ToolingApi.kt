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

    fun copyInitScript(): File {
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

    fun findArtifactForClassName(projectLocation: String, className: String) {

        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val customModelBuilder = connection.model(ConfigurationDependenciesModel::class.java)
        customModelBuilder.withArguments("--init-script", copyInitScript().absolutePath)
        val customModel: ConfigurationDependenciesModel = customModelBuilder.get()

        val ideaProjectModelBuilder = connection.model(IdeaProject::class.java)
        val ideaModel = ideaProjectModelBuilder.get()

        for (ideaModule in ideaModel.modules.all) {
            println(ideaModule)
            for (dependency in ideaModule.dependencies) {
                if (dependency is IdeaSingleEntryLibraryDependency) {
                    val libDep = dependency
                    println("Library Dependency: " + libDep)
                    try {
                        val classNamesFromJarFile = getClassNamesFromJarFile(libDep.file)
                        println(classNamesFromJarFile)
                        if (classNamesFromJarFile.contains(className)) {
                            println("Class name found: " + libDep.source)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (dependency is IdeaModuleDependency) {
                    val modDep = dependency
                    println("Module Dependency: " + modDep.targetModuleName)
                } else {
                    println("xDependency: " + dependency)
                }
            }
            println()
            println()
        }

        for (libDep in customModel.projectPluginMap()["all_plugin_jars_path"]!!) {

            try {
                val classNamesFromJarFile = getClassNamesFromJarFile(File(libDep))
                if (classNamesFromJarFile.contains(className)) {
                    println("Class name found: " + libDep)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        println()
        println()
    }


    fun findModuleDependencies(projectLocation: String, moduleName: String): List<String> {
        val connection: ProjectConnection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val ideaProject = connection.use { it.getModel(IdeaProject::class.java) }

        val module = ideaProject.children.firstOrNull { it.name == moduleName }
        if (module == null) {
            throw RuntimeException("Module '$moduleName' does not exist")
        }

        for (dependency in module.dependencies) {
            println(" * $dependency")
        }



        return emptyList()
    }

    fun showDependencies(projectLocation: String, moduleName: String) {
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val modelBuilder = connection.model(
            IdeaProject::class.java
        )
        val project = modelBuilder.get()


//        if (moduleName != "") {
//            showDependencies(project.modules.first { it.name == moduleName }.gradleProject.projectDirectory.path, "")
//        }

        if (moduleName.isBlank()) {
            println("-----------------> Listing modules for path: [$projectLocation] <----------------")
            for (module in project.modules) {
                System.out.printf("    -----------------> Module %s <----------------%n", module.name)
                for (dependency in module.dependencies) {
                    System.out.printf("Dependency: %s, scope: %s%n", dependency, dependency.scope)
                }
            }
            println("-----------------> Finishing <----------------")
        }
    }

    fun getDependencies(projectLocation: String): Collection<IdeaDependency> {
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        val modelBuilder = connection.model(IdeaProject::class.java)
        val project = modelBuilder.get()

        val gpmodelBuilder = connection.model(GradleProject::class.java)
        val gradleProject = gpmodelBuilder.get()
//        gradleProject.buildScript.plu

        return project.modules.asSequence().flatMap { it.dependencies.asSequence() }.toSet()

    }


    fun hey(projectLocation: String) {

        // Connect to the Gradle project
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(projectLocation))
            .connect()

        try {
            // Fetch the IdeaProject model
            val project = connection.getModel(IdeaProject::class.java)

            // Map to store module dependencies
            val moduleDependencies = mutableMapOf<String, String>()

            // Iterate over each module
            for (module in project.modules) {
                val moduleName = module.name

                for (dependency in module.dependencies) {


                    // Check dependencies of the module
                    if (dependency is IdeaModule) {
                        val depModuleName = dependency.name

                        // Store the dependency information
                        moduleDependencies[moduleName] = depModuleName

                        System.out.println("Module $moduleName depends on module $depModuleName")
                    } else {
                        println(dependency)
                    }
                }
            }
            // Print all module dependencies

            moduleDependencies.forEach { (m, d) -> println("Module $m has dependency on module $d"); }


        } finally {
            connection.close()
        }
    }

    fun getClassNamesFromJarFile(givenFile: File): Set<String> {
        val classNames: MutableSet<String> = HashSet()
        JarFile(givenFile).use { jarFile ->
            val e: Enumeration<JarEntry> = jarFile.entries()
            while (e.hasMoreElements()) {
                val jarEntry: JarEntry = e.nextElement()
                if (jarEntry.name.endsWith(".class")) {
                    val className: String = jarEntry.name
                        .replace("/", ".")
                        .replace(".class", "")
                    classNames.add(className)
                }
            }
            return classNames
        }
    }


}
