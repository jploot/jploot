package jploot.config.model;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
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
	 * Jploot runtime location
	 */
	Path jplootHome();
	/**
	 * Available runtimes.
	 */
	@Value.Redacted
	Set<JavaRuntime> runtimes();
	/**
	 * Available applications.
	 */
	@Value.Redacted
	Set<JplootApplication> applications();
	/**
	 * Maven repositories
	 */
	@Value.Redacted
	List<URI> repositories();

	default Path jplootBase() {
		return Path.of(".").toAbsolutePath().relativize(location().toAbsolutePath().getParent());
	}

	default Path repository() {
		return jplootBase().resolve("repository");
	}

}
