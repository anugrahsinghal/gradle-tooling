import org.gradle.plugins.ide.internal.tooling.idea.DefaultIdeaProject;

import java.util.Map;
import java.util.Set;

/**
 * This is a custom tooling model serving information about resolved dependencies.
 */
public interface ConfigurationDependenciesModel {
    Map<String, Set<String>> projectPluginMap();
    Set<String> pluginJarPaths();
    String debug();
}
