import intellij.ExternalDependency;
import intellij.resolver.GradleDependencyResolver;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal;
import org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.diagnostics.internal.ConfigurationDetails;
import org.gradle.api.tasks.diagnostics.internal.ProjectDetails;
import org.gradle.api.tasks.diagnostics.internal.ProjectsWithConfigurations;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurationDependenciesModelBuilder implements ToolingModelBuilder {
	@Override
	public boolean canBuild(String modelName) {
		return modelName.equals(ConfigurationDependenciesModel.class.getName());
	}

	@Override
	public Object buildAll(String modelName, Project rootProject) {
		// todo: include in the CustomModel
		Map<Project, Set<String>> projectToPluginMap = getProjectToPluginMapping(rootProject);
		Map<String, Set<String>> projectToPluginMapOutput = new HashMap<>();
		projectToPluginMap.forEach((project, plugins) -> projectToPluginMapOutput.put(project.getName(), plugins));

		Set<File> pluginJars = rootProject.getAllprojects().stream()
				.map(Project::getPlugins)
				.flatMap(it -> pluginJarLocation(it).stream())
				.collect(Collectors.toSet());

		Set<String> allPluginsJarPaths = pluginJars.stream().map(File::getAbsolutePath).collect(Collectors.toSet());

		// Given a module name, list all other modules that are added as dependencies.
		// For example, in the Signal Android project, the video-app module depends on the video module and core-util module.
		DefaultDependencyHandler dependencies = (DefaultDependencyHandler) rootProject.getDependencies();

//		IdeaModelBuilder ideaModelBuilder = new IdeaModelBuilder(null);
//
//		DefaultIdeaProject defaultIdeaProject = ideaModelBuilder.buildAll(modelName, rootProject);
		List<String> output = new ArrayList<>();
		for (Project project : rootProject.getAllprojects()) {
			output.add("-------" + project.getName() + "-------");
			for (Configuration configuration : project.getConfigurations()) {

				try {
					if (configuration.isCanBeResolved() && !configuration.getName().startsWith("_internal")) {
						output.add("      ------" + configuration.getName() + "-------");
						FileCollection dependencyFileCollection = configuration.fileCollection(configuration.getAllDependencies().toArray(new Dependency[0]));
						for (File file : dependencyFileCollection) {
							output.add("            -------" + file.getAbsolutePath());
						}
					}
				} catch (Exception e) {
					output.add("ERR: " + configuration.getName() + ": " + e.getMessage());
					for (Dependency dependency : configuration.getAllDependencies()) {
						output.add("            ERR-------" + dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion() + "__" + dependency.getGroup());
					}

				}
				for (PublishArtifact artifact : configuration.getAllArtifacts()) {
					output.add("            ART-------" + artifact + "TYPE: " + artifact.getType());
				}
			}
		}

		ProjectsWithConfigurations<ProjectDetails.ProjectNameAndPath, ConfigurationDetails> projectNameAndPathConfigurationDetailsProjectsWithConfigurations = computeProjectsWithConfigurations(rootProject);

		StringBuilder output2 = new StringBuilder();
		for (Project project : rootProject.getAllprojects()) {
			var classpathConfiguration = project.getBuildscript().getConfigurations().findByName("classpath");

			Collection<ExternalDependency> dependencies1 = new GradleDependencyResolver(rootProject).resolveDependencies(classpathConfiguration);

			String s = dependencies1.stream().map(it -> it.toString()).collect(Collectors.joining("\n"));

			Map<String, Collection<ExternalDependency>> stringCollectionMap = new DependencyResolveHelper().collectDependencies(project);

			output2.append("PROJECT::" + project.getName() + "----------\n")
//					.append(s)
					.append("\nMAP_STUFF::" + stringCollectionMap + "\n")
					.append("\n\n");
		}

		return new DefaultDependenciesModel(
				output2,
//				"ProjectToPluginMap::\n" + String.join("\n", output) + "\n\n" + projectNameAndPathConfigurationDetailsProjectsWithConfigurations + "\n\n",
				projectToPluginMapOutput,
				allPluginsJarPaths
		);
	}

	private @NotNull Map<Project, Set<String>> getProjectToPluginMapping(Project rootProject) {
		Set<File> pluginJars = rootProject.getAllprojects().stream()
				.map(Project::getPlugins)
				.flatMap(it -> pluginJarLocation(it).stream())
				.collect(Collectors.toSet());

		// not sure - can plugins have conflicting names ?
		// if yes then might need to update to handle that
		Set<String> allAvailablePlugins = pluginJars.stream()
				.map(this::readGradlePluginFromJar)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		Set<String> pluginNames = allAvailablePlugins.stream()
				.map(it -> it.replace("META-INF/gradle-plugins/", "").replaceAll(".properties$", ""))
				.collect(Collectors.toSet());

		Map<Project, Set<String>> projectToPluginMap = new HashMap<>();
		for (Project project : rootProject.getAllprojects()) {
			PluginContainer projectPluginsContainer = project.getPlugins();

			Set<String> actualPlugins = pluginNames.stream()
					.map(name -> {
						try {
							if (projectPluginsContainer.hasPlugin(name)) {
								return name;
							} else return null;
						} catch (Exception e) {
							return null;
						}
					}).filter(Objects::nonNull)
					.collect(Collectors.toSet());

			projectToPluginMap.computeIfAbsent(project, k -> new HashSet<>()).addAll(actualPlugins);
		}
		return projectToPluginMap;
	}

	Collection<File> pluginJarLocation(PluginContainer pluginsContainer) {
		return pluginsContainer.stream()
				.flatMap(it -> getJarFile(it).stream())
				.collect(Collectors.toSet());
	}

	private Set<String> readGradlePluginFromJar(File jarFile) {
		Set<String> pluginFilePath = new HashSet<>();
		try (JarFile jar = new JarFile(jarFile)) {
			final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			while (entries.hasMoreElements()) {
				JarEntry jarEntry = entries.nextElement();
				final String name = jarEntry.getName();
				if (name.contains("META-INF/gradle-plugins/") && name.endsWith(".properties")) {
					pluginFilePath.add(name);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not process JarFile", e);
		}
		return pluginFilePath;
	}

	private Optional<File> getJarFile(Plugin plugin) {
		final File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		if (jarFile.isFile()) {  // Run with JAR file
			return Optional.of(jarFile);
		}
		return Optional.empty();
	}

	String printMap(Map<Project, Set<String>> map) {
		String s = map.entrySet().stream().map(e -> e.getKey().getName()).collect(Collectors.joining("\n"));
		return map.entrySet().stream().map(e -> e.getKey().getName() + "::\n" + String.join("\n", e.getValue())).collect(Collectors.joining("\n\n"));
	}

	void renderHTMLReverseEngineer(Project project) {
		ProjectsWithConfigurations<ProjectDetails.ProjectNameAndPath, ConfigurationDetails> projectsWithConfigurations = computeProjectsWithConfigurations(project);
	}

	private ProjectsWithConfigurations<ProjectDetails.ProjectNameAndPath, ConfigurationDetails> computeProjectsWithConfigurations(Project project) {
		Map<ProjectDetails.ProjectNameAndPath, Iterable<ConfigurationDetails>> details = new LinkedHashMap<>();
		List<Project> projects = List.of(project);
		projects.forEach(_project -> {
			ProjectDetails.ProjectNameAndPath projectDetails = ProjectDetails.withNameAndPath(_project);
			Iterable<ConfigurationDetails> configurationDetails = getConfigurationsWhichCouldHaveDependencyInfo(_project).collect(Collectors.toList());
			details.put(projectDetails, configurationDetails);
		});
		return new ProjectsWithConfigurations<>() {
			@Override
			public Set<ProjectDetails.ProjectNameAndPath> getProjects() {
				return details.keySet();
			}

			@Override
			public Iterable<ConfigurationDetails> getConfigurationsFor(ProjectDetails.ProjectNameAndPath project1) {
				return details.getOrDefault(project1, Collections.emptySet());
			}
		};
	}

	private static Stream<? extends ConfigurationDetails> getConfigurationsWhichCouldHaveDependencyInfo(Project project) {
		return project.getConfigurations().stream()
				.map(ConfigurationInternal.class::cast)
				.filter(c -> c.isDeclarableByExtension())
				.map(ConfigurationDetails::of);
	}
}
