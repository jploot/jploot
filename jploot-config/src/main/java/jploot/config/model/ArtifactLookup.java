package jploot.config.model;

import java.nio.file.Path;
import java.util.Optional;

import org.immutables.value.Value;

import jploot.exceptions.JplootArtifactFailure;

/**
 * Artifact lookup result
 */
@Value.Immutable
public interface ArtifactLookup {

	/**
	 * Resolved artifact.
	 */
	JplootArtifact artifact();
	/**
	 * Artifact resolved path; available only if resolution is not failed.
	 */
	Optional<Path> path();
	/**
	 * Source type for artifact; a dependency may allow multiple sources. This value allow to know with source
	 * is used to resolve artifact. Available only if resolution is not failed.
	 */
	Optional<DependencySource> source();
	/**
	 * Details on resolution failure, if resolution is failed.
	 */
	Optional<JplootArtifactFailure> failure();

}
