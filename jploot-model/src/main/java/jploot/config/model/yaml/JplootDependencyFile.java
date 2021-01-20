package jploot.config.model.yaml;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@FileStyle
@JsonDeserialize(builder = ImmutableJplootDependencyFile.Builder.class)
public interface JplootDependencyFile {

	Optional<String> groupId();
	Optional<String> artifactId();
	Optional<String> version();

}
