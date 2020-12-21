package jploot.core.runner.spi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.exceptions.JplootArtifactFailure;
import jploot.config.model.ArtifactLookups;
import jploot.config.model.DependencySource;
import jploot.config.model.ImmutableArtifactLookup;
import jploot.config.model.ImmutableArtifactLookups;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootArtifact;
import jploot.config.model.JplootConfig;
import jploot.config.model.MavenRepository;

public class ArtifactResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactResolver.class);

	private final PathHandler pathHandler;

	public ArtifactResolver(PathHandler pathHandler) {
		super();
		this.pathHandler = pathHandler;
	}

	public ArtifactLookups resolve(JplootConfig config, JplootApplication application) {
		LOGGER.debug("Resolving {} in {}", application, config);
		
		ImmutableArtifactLookups.Builder artifactLookupsBuilder = ImmutableArtifactLookups.builder()
				.application(application);
		List<JplootArtifact> artifacts = new ArrayList<>();
		artifacts.add(application);
		artifacts.addAll(application.dependencies());
		
		for (JplootArtifact artifact : artifacts) {
			ImmutableArtifactLookup.Builder artifactLookupBuilder = ImmutableArtifactLookup.builder()
					.artifact(artifact);
			if (artifact.allowedSources().contains(DependencySource.JPLOOT_EMBEDDED)) {
				// TODO remove placeholder
				artifactLookupBuilder.source(DependencySource.JPLOOT_EMBEDDED).path(Path.of("placeholder"));
			} else {
				Path path;
				// TODO allow multiple sources
				DependencySource source = artifact.allowedSources().iterator().next();
				switch(source) {
				case JPLOOT:
					path = resolveJplootPath(config, artifact);
					break;
				case MAVEN:
					path = resolveMavenPath(config, artifact);
					break;
				default:
					// TODO manage edge-case
					throw new IllegalStateException();
				}
				try {
					pathHandler.isValidArtifact(path, application, artifact, config);
					artifactLookupBuilder.source(DependencySource.JPLOOT).path(path);
				} catch (JplootArtifactFailure failure) {
					artifactLookupBuilder.failure(failure);
				}
				artifactLookupsBuilder.putLookups(artifact, artifactLookupBuilder.build());
			}
		}
		
		return artifactLookupsBuilder.build();
	}

	private Path resolveJplootPath(JplootConfig config, JplootArtifact artifact) {
		return resolvePath(config.repository(), artifact);
	}

	private Path resolveMavenPath(JplootConfig config, JplootArtifact artifact) {
		// TODO: implement multi-repo
		MavenRepository repository = config.mavenRepositories().iterator().next();
		return resolvePath(repository.location(), artifact);
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
