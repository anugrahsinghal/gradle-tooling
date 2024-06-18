import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.internal.artifacts.DefaultProjectComponentIdentifier;
import org.gradle.api.internal.artifacts.result.DefaultResolvedDependencyResult;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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

		// https://stackoverflow.com/questions/34641631/how-can-i-get-a-list-of-my-projects-dependencies-in-a-flattened-form-using-gradl
		Map<String, Set<String>> projectToInternalDependencies = new HashMap<>();
		Map<String, Set<String>> projectToExternalDependencyPaths = new HashMap<>();

		for (Project subProject : rootProject.getAllprojects()) {
			Configuration releaseRuntimeClasspath = subProject.getConfigurations().findByName("releaseRuntimeClasspath");
			if (releaseRuntimeClasspath == null) {
				continue;
			}
			ResolvableDependencies resolvableDependencies = releaseRuntimeClasspath.getIncoming();

			Set<DefaultResolvedDependencyResult> dependencyResults = resolvableDependencies.getResolutionResult().getAllDependencies()
					.stream()
					.filter(DefaultResolvedDependencyResult.class::isInstance)
					.map(DefaultResolvedDependencyResult.class::cast)
					.collect(Collectors.toSet());

			Set<String> internalDependencies = dependencyResults
					.stream()
					.map(it -> it.getSelected().getId())
					.filter(DefaultProjectComponentIdentifier.class::isInstance)
					.map(DefaultProjectComponentIdentifier.class::cast)
					// .map(DefaultProjectComponentIdentifier.class::cast) // for external dependency without path
					.map(DefaultProjectComponentIdentifier::getProjectName)
					.collect(Collectors.toSet());
			projectToInternalDependencies.put(subProject.getName(), internalDependencies);

			Attribute<String> artifactType = Attribute.of("artifactType", String.class);
			Set<String> externalDependencyPaths = resolvableDependencies
					.artifactView(ac -> ac.getAttributes().attribute(artifactType, "jar"))
					.getArtifacts().getArtifacts()
					.stream()
					// todo: change this hacky way to some useful POJO
					.map(it -> it.getFile().toPath().toAbsolutePath() + "__" + it.getId().getDisplayName())
					.collect(Collectors.toSet());

			projectToExternalDependencyPaths.put(subProject.getName(), externalDependencyPaths);

		}

		return new DefaultDependenciesModel(
				"",
				projectToPluginMapOutput,
				projectToInternalDependencies,
				projectToExternalDependencyPaths
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

}
