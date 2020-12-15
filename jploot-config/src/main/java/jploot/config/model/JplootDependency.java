package jploot.config.model;

import java.util.Set;

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
	@Override
	/**
	 * Is this dependency managed as a classpath or module dependency.
	 */
	@Value.Auxiliary
	Set<DependencyType> types();
	/**
	 * Which sources are allowed to lookup this dependency.
	 */
	@Override
	@Value.Auxiliary
	Set<DependencySource> allowedSources();

}
