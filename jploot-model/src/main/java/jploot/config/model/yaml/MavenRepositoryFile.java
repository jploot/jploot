package jploot.config.model.yaml;

import java.nio.file.Path;

public interface MavenRepositoryFile {

	/**
	 * A name to reference this repository.
	 */
	String name();
	/**
	 * Maven repository location (like ~/.m2/repository).
	 */
	Path location();

}
