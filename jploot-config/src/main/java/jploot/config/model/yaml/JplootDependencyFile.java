package jploot.config.model.yaml;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jploot.config.model.DependencySource;
import jploot.config.model.DependencyType;

@Value.Immutable
@FileStyle
@JsonDeserialize(builder = ImmutableJplootDependencyFile.Builder.class)
public interface JplootDependencyFile {

	Optional<String> groupId();
	Optional<String> artifactId();
	Optional<String> version();
	Optional<Set<DependencyType>> types();
	Optional<Set<DependencySource>> allowedSources();

}
