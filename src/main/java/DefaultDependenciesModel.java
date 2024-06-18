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
	private final Map<String, Set<String>> projectToInternalDependencies;
	private final Map<String, Set<String>> projectToExternalDependencyPaths;

	public DefaultDependenciesModel(Object debugger, Map<String, Set<String>> projectToPluginMapping, Set<String> pluginJarFiles, Map<String, Set<String>> projectToInternalDependencies, Map<String, Set<String>> projectToExternalDependencyPaths) {
		this.myDebugger = debugger;
		this.projectToPluginMapping = projectToPluginMapping;
		this.pluginJarFiles = pluginJarFiles;
		this.projectToInternalDependencies = projectToInternalDependencies;
		this.projectToExternalDependencyPaths = projectToExternalDependencyPaths;
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

	@Override
	public Map<String, Set<String>> projectToInternalDependencies() {
		return projectToInternalDependencies;
	}

	@Override
	public Map<String, Set<String>> projectToExternalDependencyPaths() {
		return projectToExternalDependencyPaths;
	}

	@Override
	public String toString() {
		return "DefaultDependenciesModel{" +
				"\nmyDebugger=" + myDebugger +
				", \nprojectToPluginMapping=" + projectToPluginMapping +
				", \npluginJarFiles=" + pluginJarFiles +
				", \nprojectToInternalDependencies=" + projectToInternalDependencies +
				", \nprojectToExternalDependencyPaths=" + projectToExternalDependencyPaths +
				'}';
	}
}
