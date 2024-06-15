import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * This is the implementation of the custom tooling model. It must be serializable and must have methods and properties that
 * are compatible with the custom tooling model interface. It may or may not implement the custom tooling model interface.
 */
public class DefaultDependenciesModel implements Serializable {
	private final Object myDebugger;
	private final Map<String, Set<String>> projectToPluginMapping;

	public DefaultDependenciesModel(Object s, Map<String, Set<String>> projectToPluginMapping) {
		this.myDebugger = s;
		this.projectToPluginMapping = projectToPluginMapping;
	}

	public String sayHello() {
		return "Hello world! " + myDebugger;
	}

	private Map<String, Set<String>> getProjectToPluginMapping() {
		return projectToPluginMapping;
	}


}
