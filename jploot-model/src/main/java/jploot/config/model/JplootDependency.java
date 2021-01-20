package jploot.config.model;

import org.immutables.value.Value;

/**
 * A Jploot dependency, a jar artifact.
 */
@Value.Immutable
public interface JplootDependency extends JplootArtifact {

	@Override
	String groupId();
	@Override
	String artifactId();
	@Override
	String version();

}
