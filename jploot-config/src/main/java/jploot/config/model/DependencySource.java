package jploot.config.model;

public enum DependencySource {

	/**
	 * Used for artifacts packaged in application archive.
	 */
	EMBEDDED,
	/**
	 * Artifact stored in jploot local repository.
	 */
	JPLOOT,
	/**
	 * Used for Jploot native items; no classpath/module is needed.
	 */
	JPLOOT_EMBEDDED,
	/**
	 * Artifact stored in maven local repository (~/.m2/repository).
	 */
	MAVEN;

}
