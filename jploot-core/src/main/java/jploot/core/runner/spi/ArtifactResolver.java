package jploot.core.runner.spi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.ArtifactLookups;
import jploot.config.model.DependencySource;
import jploot.config.model.ImmutableArtifactLookup;
import jploot.config.model.ImmutableArtifactLookups;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootArtifact;
import jploot.config.model.JplootConfig;
import jploot.exceptions.JplootArtifactFailure;

public class ArtifactResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactResolver.class);

	private final PathHandler pathHandler;

	public ArtifactResolver(PathHandler pathHandler) {
		super();
		this.pathHandler = pathHandler;
	}

	public ArtifactLookups resolve(JplootConfig config, JplootApplication application) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("⏳ Resolving {} dependencies", application.asSpec());
		}
		
		ImmutableArtifactLookups.Builder artifactLookupsBuilder = ImmutableArtifactLookups.builder()
				.application(application);
		List<JplootArtifact> artifacts = new ArrayList<>();
		artifacts.add(application);
		artifacts.addAll(application.dependencies());
		
		for (JplootArtifact artifact : artifacts) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("⏳ Resolving dependency {}", artifact.asSpec());
			}
			ImmutableArtifactLookup.Builder artifactLookupBuilder = ImmutableArtifactLookup.builder()
					.artifact(artifact);
			Path path;
			// TODO allow multiple sources
			DependencySource source = artifact.allowedSources().iterator().next();
			switch(source) {
			case JPLOOT:
				path = resolveJplootPath(config, artifact);
				break;
			default:
				// TODO manage edge-case
				throw new IllegalStateException();
			}
			try {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("⏳ Validating dependency {}", path);
				}
				pathHandler.isValidArtifact(path, application, artifact, config);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("🔵 Validating dependency {} done", path);
				}
				artifactLookupBuilder.source(DependencySource.JPLOOT).path(path);
			} catch (JplootArtifactFailure failure) {
				artifactLookupBuilder.failure(failure);
			}
			artifactLookupsBuilder.putLookups(artifact, artifactLookupBuilder.build());
		}
		
		LOGGER.trace("🔵 Resolving {} dependencies done", application.asSpec());
		
		return artifactLookupsBuilder.build();
	}

	private Path resolveJplootPath(JplootConfig config, JplootArtifact artifact) {
		return resolvePath(config.repository(), artifact);
	}

	private Path resolvePath(Path root, JplootArtifact artifact) {
		List<Path> fragments = new ArrayList<>();
		fragments.add(root);
		fragments.add(Path.of(artifact.groupId().replace(".", "/")));
		fragments.add(Path.of(artifact.artifactId()));
		fragments.add(Path.of(artifact.version()));
		fragments.add(Path.of(String.format("%s-%s.jar",
				artifact.artifactId(),
				artifact.version())));
		
		Path path = fragments.stream().reduce((first, second) -> first.resolve(second)).get(); //NOSONAR
		return path;
	}

}
