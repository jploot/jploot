package jploot.config.model.yaml;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@FileStyle
@JsonDeserialize(builder = ImmutableJplootConfigFile.Builder.class)
public interface JplootConfigFile {

	@JsonInclude(value = Include.NON_EMPTY)
	@Value.Auxiliary
	Optional<Set<JavaRuntimeFile>> runtimes();
	@JsonInclude(value = Include.NON_EMPTY)
	@Value.Auxiliary
	Optional<Set<JplootApplicationFile>> applications();
	@JsonInclude(value = Include.NON_EMPTY)
	@Value.Auxiliary
	Optional<Set<MavenRepositoryFile>> mavenRepositories();

}
