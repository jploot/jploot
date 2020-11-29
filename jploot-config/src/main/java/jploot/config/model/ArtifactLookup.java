package jploot.config.model;

import java.nio.file.Path;
import java.util.Optional;

import org.immutables.value.Value;

import jploot.config.exceptions.JplootArtifactFailure;

@Value.Immutable
public interface ArtifactLookup {

	JplootArtifact artifact();
	Optional<Path> path();
	Optional<DependencySource> source();
	Optional<JplootArtifactFailure> failure();

}
