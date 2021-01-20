package jploot.config.model;

/**
 * Interface to share application and dependency artifact's attributes.
 */
public interface JplootArtifact {

	String groupId();
	String artifactId();
	String version();

	default String asSpec() {
		return String.format("%s:%s:%s", groupId(), artifactId(), version());
	}

}
