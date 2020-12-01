package jploot.config.model;

import java.util.Set;

import org.immutables.value.Value;

@Value.Immutable
public interface JplootDependency extends JplootArtifact {

	@Override
	String groupId();
	@Override
	String artifactId();
	@Override
	String version();
	@Override
	@Value.Auxiliary
	Set<DependencyType> types();
	@Value.Auxiliary
	Set<DependencySource> allowedSources();

}
