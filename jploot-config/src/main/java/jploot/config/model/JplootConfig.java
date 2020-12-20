package jploot.config.model;

import java.nio.file.Path;
import java.util.Set;

import org.immutables.value.Value;

/**
 * Jploot configuration.
 */
@Value.Immutable
public interface JplootConfig {

	/**
	 * Configuration file location.
	 */
	Path location();
	/**
	 * Available runtimes.
	 */
	@Value.Auxiliary
	Set<JavaRuntime> runtimes();
	/**
	 * Available applications.
	 */
	@Value.Auxiliary
	Set<JplootApplication> applications();
	/**
	 * Maven repositories.
	 */
	@Value.Auxiliary
	Set<MavenRepository> mavenRepositories();

	default Path jplootBase() {
		return location().getParent();
	}

	default Path repository() {
		return jplootBase().resolve("repository");
	}

}
