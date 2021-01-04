package jploot.config.model.yaml;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@FileStyle
@JsonDeserialize(builder = ImmutableJplootApplicationFile.Builder.class)
public interface JplootApplicationFile {

	Optional<String> name();
	Optional<String> groupId();
	Optional<String> artifactId();
	Optional<String> version();
	@Auxiliary
	Optional<String> description();
	@Auxiliary
	Optional<Set<JplootDependencyFile>> dependencies();
	@Auxiliary
	Optional<String> mainClass();
	@Auxiliary
	Optional<Set<String>> launchers();

}
