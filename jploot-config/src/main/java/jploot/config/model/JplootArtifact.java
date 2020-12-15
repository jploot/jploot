package jploot.config.model;

import java.util.Set;

/**
 * Interface to share application and dependency artifact's attributes.
 */
public interface JplootArtifact {

	String groupId();
	String artifactId();
	String version();
	Set<DependencyType> types();
	Set<DependencySource> allowedSources();

}
