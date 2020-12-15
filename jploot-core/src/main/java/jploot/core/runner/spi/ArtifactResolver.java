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

	private static final String JPLOOT_BASE_ARTIFACTS_PATH = "artifacts";

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
					path = resolveJplootPath(config, application);
					break;
				case MAVEN:
					path = resolveMavenPath(config, application);
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

	private Path resolveJplootPath(JplootConfig config, JplootApplication application) {
		List<Path> fragments = new ArrayList<>();
		fragments.add(config.jplootBase());
		fragments.add(Path.of(JPLOOT_BASE_ARTIFACTS_PATH));
		fragments.add(Path.of(String.format("%s-%s-%s.jar",
				application.groupId(),
				application.artifactId(),
				application.version())));
		
		Path path = fragments.stream().reduce((first, second) -> first.resolve(second)).get(); //NOSONAR
		return path;
	}

	private Path resolveMavenPath(JplootConfig config, JplootApplication application) {
		// TODO: implement multi-repo
		MavenRepository repository = config.mavenRepositories().iterator().next();
		List<Path> fragments = new ArrayList<>();
		fragments.add(repository.location());
		fragments.add(Path.of(application.groupId().replace(".", "/")));
		fragments.add(Path.of(application.artifactId()));
		fragments.add(Path.of(application.version()));
		fragments.add(Path.of(String.format("%s-%s.jar",
				application.artifactId(),
				application.version())));
		
		Path path = fragments.stream().reduce((first, second) -> first.resolve(second)).get(); //NOSONAR
		return path;
	}

}
