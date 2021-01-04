package jploot.config.model;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * A Jploot application, made of a main artifact, dependencies and a main class.
 */
@Value.Immutable
public interface JplootApplication extends JplootArtifact {

	/**
	 * Application name used to reference application. Used for CLI invocation.
	 */
	String name();
	/**
	 * Application artifact groupId.
	 */
	@Override
	String groupId();
	/**
	 * Application artifact artifactId.
	 */
	@Override
	String artifactId();
	/**
	 * Application artifact version.
	 */
	@Override
	String version();
	/**
	 * Application description; for command-line documentation.
	 */
	Optional<String> description();
	/**
	 * Application dependencies.
	 * @return
	 */
	Set<JplootDependency> dependencies();
	/**
	 * Application main class; may be absent if jar provides a MANIFEST declaration
	 */
	Optional<String> mainClass();
	/**
	 * Basename for launchers
	 */
	Optional<Set<String>> launchers();

	default String toDebug() {
		ToStringHelper artifactItems = addJplootArtifact(MoreObjects.toStringHelper(getClass()));
		return artifactItems
				.add("mainClass", mainClass())
				.add("description", description())
				.add("dependencies", dependencies())
				.toString();
	}

}
