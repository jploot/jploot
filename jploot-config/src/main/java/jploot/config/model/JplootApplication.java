package jploot.config.model;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface JplootApplication {

	String name();
	String groupId();
	String artifactId();
	String version();
	Optional<String> description();
	Optional<String> mainClass();

}
