package jploot.config.model.yaml;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@FileStyle
@JsonDeserialize(builder = ImmutableJplootConfigFile.Builder.class)
public interface JplootConfigFile {

	@Value.Auxiliary
	Optional<Set<JavaRuntimeFile>> runtimes();
	@Value.Auxiliary
	Optional<Set<JplootApplicationFile>> applications();
	@Value.Auxiliary
	Optional<Set<MavenRepositoryFile>> mavenRepositories();

}
