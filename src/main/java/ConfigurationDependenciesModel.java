import org.gradle.plugins.ide.internal.tooling.idea.DefaultIdeaProject;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * This is a custom tooling model serving information about resolved dependencies.
 */
public interface ConfigurationDependenciesModel {
    String debug();
    Set<String> pluginJarPaths();
    Map<String, Set<String>> projectPluginMap();
    Map<String, Set<String>> projectToInternalDependencies();
    Map<String, Set<File>> projectToExternalDependencyPaths();
}
