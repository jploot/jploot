package jploot.config.model;

import java.nio.file.Path;

import org.immutables.value.Value;

/**
 * Jploot command line arguments
 */
@Value.Immutable
public interface ArgumentConfig {

	/**
	 * Configuration location
	 */
	Path location();

}
