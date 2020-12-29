package jploot.config.model;

import com.google.common.base.MoreObjects;

/**
 * Interface to share application and dependency artifact's attributes.
 */
public interface JplootArtifact {

	String groupId();
	String artifactId();
	String version();

	default MoreObjects.ToStringHelper addJplootArtifact(MoreObjects.ToStringHelper helper) {
		return helper
			.add("groupId", groupId())
			.add("artifactId", artifactId())
			.add("version", version());
	}

	default String asSpec() {
		return String.format("%s:%s:%s", groupId(), artifactId(), version());
	}

}
