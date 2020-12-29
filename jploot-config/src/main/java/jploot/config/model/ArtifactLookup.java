package jploot.config.model;

import java.nio.file.Path;
import java.util.Optional;

import org.immutables.value.Value;

import jploot.exceptions.JplootArtifactFailure;
import jploot.exceptions.JplootIllegalStateException;

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
	 * Details on resolution failure, if resolution is failed.
	 */
	Optional<JplootArtifactFailure> failure();

	default Path resolvedPath() {
		Optional<Path> path = path();
		if (path.isEmpty()) {
			throw new JplootIllegalStateException(
					String.format("resolvedPath called on an unresolved lookup %s", this));
		} else {
			return path.get();
		}
	}

	default JplootArtifactFailure triggeredFailure() {
		Optional<JplootArtifactFailure> failure = failure();
		if (failure.isEmpty()) {
			throw new JplootIllegalStateException(
					String.format("triggeredFailure called on a resolved lookup %s", this));
		} else {
			return failure.get();
		}
	}

}
