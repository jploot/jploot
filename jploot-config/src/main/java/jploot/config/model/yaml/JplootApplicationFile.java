package jploot.config.model.yaml;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@FileStyle
@JsonDeserialize(builder = ImmutableJplootApplicationFile.Builder.class)
public interface JplootApplicationFile {

	Optional<String> name();
	Optional<String> groupId();
	Optional<String> artifactId();
	Optional<String> version();
	Optional<String> description();
	Optional<Set<JplootDependencyFile>> dependencies();
	Optional<String> mainClass();

}
