package jploot.config.model;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

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
	@Value.Redacted
	Optional<String> description();
	/**
	 * Application dependencies.
	 * @return
	 */
	@Value.Redacted
	Set<JplootDependency> dependencies();
	/**
	 * Application main class; may be absent if jar provides a MANIFEST declaration
	 */
	@Value.Redacted
	Optional<String> mainClass();
	/**
	 * Basename for launchers
	 */
	@Value.Redacted
	Optional<Set<String>> launchers();

}
