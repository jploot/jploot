package jploot.config.model;

import java.util.Set;

import com.google.common.base.MoreObjects;

/**
 * Interface to share application and dependency artifact's attributes.
 */
public interface JplootArtifact {

	String groupId();
	String artifactId();
	String version();
	Set<DependencyType> types();
	Set<DependencySource> allowedSources();

	default MoreObjects.ToStringHelper addJplootArtifact(MoreObjects.ToStringHelper helper) {
		return helper
			.add("groupId", groupId())
			.add("artifactId", artifactId())
			.add("version", version());
	}

	default MoreObjects.ToStringHelper addMoreJplootArtifact(MoreObjects.ToStringHelper helper) {
		return helper
			.add("types", types())
			.add("allowedSources", allowedSources());
	}

	default String asSpec() {
		return String.format("%s:%s:%s", groupId(), artifactId(), version());
	}

}
