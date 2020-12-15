package jploot.config.model;

import java.nio.file.Path;

import org.immutables.value.Value;

/**
 * A maven repository
 */
@Value.Immutable
public interface MavenRepository {

	/**
	 * A name to reference this repository.
	 */
	String name();
	/**
	 * Maven repository location (like ~/.m2/repository).
	 */
	Path location();

}
