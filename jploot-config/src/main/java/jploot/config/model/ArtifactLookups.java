package jploot.config.model;

import java.util.Map;
import java.util.stream.Stream;

import org.immutables.value.Value;

/**
 * A collection of {@link ArtifactLookup} for an {@link JplootApplication}.
 */
@Value.Immutable
public interface ArtifactLookups {

	/**
	 * Resolved application.
	 */
	JplootApplication application();
	Map<JplootArtifact, ArtifactLookup> lookups();

	/**
	 * Failed artifact resolution.
	 */
	default Stream<Map.Entry<JplootArtifact, ArtifactLookup>> failedLookups() {
		return lookups().entrySet().stream().filter(e -> e.getValue().failure().isPresent());
	}
}
