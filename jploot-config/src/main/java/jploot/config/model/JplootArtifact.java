package jploot.config.model;

import java.util.Set;

public interface JplootArtifact {

	String groupId();
	String artifactId();
	String version();
	Set<DependencyType> types();
	Set<DependencySource> allowedSources();

}
