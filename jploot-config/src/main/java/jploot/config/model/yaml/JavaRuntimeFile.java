package jploot.config.model.yaml;

import java.nio.file.Path;
import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@FileStyle
@JsonDeserialize(builder = ImmutableJavaRuntimeFile.Builder.class)
public interface JavaRuntimeFile {

	Optional<String> name();
	Optional<Path> javaHome();
	Optional<String> version();

}
