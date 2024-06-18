import intellij.ExtensionJavaPluginAccessor;
import intellij.ExternalDependency;
import intellij.resolver.GradleDependencyResolver;
import intellij.resolver.GradleSourceSetDependencyResolver;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DependencyResolveHelper {

	String CLASSPATH_CONFIGURATION_NAME = "classpath";

	String resolveDependencies(Project rootProject) {
		var classpathConfiguration = rootProject.getConfigurations().findByName(CLASSPATH_CONFIGURATION_NAME);

		Collection<ExternalDependency> dependencies = new GradleDependencyResolver(rootProject).resolveDependencies(classpathConfiguration);

		return dependencies.stream().map(it -> it.toString()).collect(Collectors.joining("\n"));
	}

	@NotNull Map<String, Collection<ExternalDependency>> collectDependencies(
			@NotNull Project project
	) {
//		if (!Boolean.getBoolean("idea.resolveSourceSetDependencies")) {
//			return new LinkedHashMap<>();
//		}
		@NotNull ExtensionJavaPluginAccessor result1 = new ExtensionJavaPluginAccessor(project);

		@Nullable SourceSetContainer sourceSets = null;
		JavaPluginExtension javaExtension = project.getExtensions().findByType(JavaPluginExtension.class);
		if (javaExtension != null) {
			sourceSets = javaExtension.getSourceSets();
		}
		if (sourceSets == null) {
			return new LinkedHashMap<>();
		}
		GradleSourceSetDependencyResolver dependencyResolver = new GradleSourceSetDependencyResolver(project);
		Map<String, Collection<ExternalDependency>> result = new LinkedHashMap<>();
		sourceSets.forEach(sourceSet -> {
			Collection<ExternalDependency> dependencies = dependencyResolver.resolveDependencies(sourceSet);
			result.put(sourceSet.getName(), dependencies);
		});
		return result;
	}
}
