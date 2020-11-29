package jploot.config.model;

import java.util.Map;
import java.util.stream.Stream;

import org.immutables.value.Value;

@Value.Immutable
public interface ArtifactLookups {

	JplootApplication application();
	Map<JplootArtifact, ArtifactLookup> lookups();

	default Stream<Map.Entry<JplootArtifact, ArtifactLookup>> failedLookups() {
		return lookups().entrySet().stream().filter(e -> e.getValue().failure().isPresent());
	}
}
