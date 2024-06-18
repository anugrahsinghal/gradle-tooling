import org.gradle.plugins.ide.internal.tooling.idea.DefaultIdeaProject;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * This is the implementation of the custom tooling model. It must be serializable and must have methods and properties that
 * are compatible with the custom tooling model interface. It may or may not implement the custom tooling model interface.
 */
public class DefaultDependenciesModel implements Serializable, ConfigurationDependenciesModel {
	private final Object myDebugger;
	private final Map<String, Set<String>> projectToPluginMapping;
	private final Set<String> pluginJarFiles;

	public DefaultDependenciesModel(Object debugger, Map<String, Set<String>> projectToPluginMapping, Set<String> pluginJarFiles, DefaultIdeaProject ideaProject) {
		this.myDebugger = debugger;
		this.projectToPluginMapping = projectToPluginMapping;
		this.pluginJarFiles = pluginJarFiles;
	}

	public Map<String, Set<String>> projectPluginMap() {
		return projectToPluginMapping;
	}

	public Set<String> pluginJarPaths() {
		return pluginJarFiles;
	}

	public String debug() {
		return myDebugger.toString();
	}


}
