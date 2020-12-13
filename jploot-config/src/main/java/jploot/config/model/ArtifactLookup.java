package jploot.config.model;

import java.nio.file.Path;
import java.util.Optional;

import org.immutables.value.Value;

import jploot.config.exceptions.JplootArtifactFailure;

/**
 * Artifact lookup result
 */
@Value.Immutable
public interface ArtifactLookup {

	/**
	 * Target artifact
	 */
	JplootArtifact artifact();
	/**
	 * Artifact resolved path
	 */
	Optional<Path> path();
	Optional<DependencySource> source();
	Optional<JplootArtifactFailure> failure();

}
