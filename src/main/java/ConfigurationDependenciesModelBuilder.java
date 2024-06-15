import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.DefaultPluginManager;
import org.gradle.api.internal.plugins.PluginImplementation;
import org.gradle.api.plugins.HelpTasksPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

		return new DefaultDependenciesModel(
						"ProjectToPluginMap::\n" + printMap(projectToPluginMap) + "\n\n"

		);
	}

	private @NotNull Map<Project, Set<String>> getProjectToPluginMapping(Project rootProject) {
		List<String> allAvailablePlugins = rootProject.getAllprojects().stream().map(this::getPluginsForProject).flatMap(Collection::stream).toList();

		List<String> pluginNames = allAvailablePlugins.stream().map(it -> it.replace("META-INF/gradle-plugins/", "").replaceAll(".properties$", "")).toList();


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

	private @NotNull List<String> getPluginsForProject(Project project) {
		return project.getPlugins().stream().map(this::getPluginNamesInsideProject).flatMap(Collection::stream).toList();
	}

	private @NotNull List<String> getPluginNamesInsideProject(Plugin plugin) {
		List<String> idk = new ArrayList<>();
		final File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		if (jarFile.isFile()) {  // Run with JAR file
			final JarFile jar;
			try {
				jar = new JarFile(jarFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			while (entries.hasMoreElements()) {
				JarEntry jarEntry = entries.nextElement();
				final String name = jarEntry.getName();
				if (name.contains("META-INF/gradle-plugins/") && name.endsWith(".properties")) {
					idk.add(name);
				}
			}
			try {
				jar.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			idk.add("IDKDKDKDK" + plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		}
		return idk;
	}

	String printMap(Map<Project, Set<String>> map) {
		String s = map.entrySet().stream().map(e -> e.getKey().getName()).collect(Collectors.joining("\n"));
		return map.entrySet().stream().map(e ->  e.getKey().getName() + "::\n" + String.join("\n", e.getValue())).collect(Collectors.joining("\n\n"));
	}

}
