package jploot.config.model;

import org.immutables.value.Value;

import com.google.common.base.MoreObjects;

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

	default String toDebug() {
		return addJplootArtifact(MoreObjects.toStringHelper(getClass()))
				.toString();
	}

}
