import java.util.Map;
import java.util.Set;

/**
 * This is a custom tooling model serving information about resolved dependencies.
 */
public interface ConfigurationDependenciesModel {

    String sayHello();
    Map<String, Set<String>> getProjectToPluginMapping();
}
