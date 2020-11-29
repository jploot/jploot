package jploot.config.model;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

@Value.Immutable
public interface JplootApplication extends JplootArtifact {

	String name();
	@Override
	String groupId();
	@Override
	String artifactId();
	@Override
	String version();
	Optional<String> description();
	Set<JplootDependency> dependencies();
	Optional<String> mainClass();

}
