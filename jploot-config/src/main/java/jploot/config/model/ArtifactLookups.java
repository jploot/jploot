package jploot.config.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
	List<ArtifactLookup> lookups();

	/**
	 * Filtered lookups.
	 */
	default Stream<ArtifactLookup> stream(Predicate<ArtifactLookup> filter) {
		return lookups().stream().filter(filter);
	}

	default Optional<ArtifactLookup> find(JplootArtifact artifact) {
		return stream(forArtifact(artifact)).findFirst();
	}

	/**
	 * Resolved artifacts.
	 */
	default List<ArtifactLookup> resolvedLookups() {
		return stream(ArtifactLookups.resolved()).collect(Collectors.toList());
	}

	/**
	 * Failed artifact resolution.
	 */
	default List<ArtifactLookup> failedLookups() {
		return stream(ArtifactLookups.failed()).collect(Collectors.toList());
	}
	
	static Predicate<ArtifactLookup> excludeArtifact(JplootArtifact artifact) {
		return i -> !i.artifact().equals(artifact);
	}
	static Predicate<ArtifactLookup> forArtifact(JplootArtifact artifact) {
		return i -> i.artifact().equals(artifact);
	}
	static Predicate<ArtifactLookup> resolved() {
		return i -> i.failure().isEmpty();
	}
	static Predicate<ArtifactLookup> failed() {
		return i -> i.failure().isPresent();
	}
}
