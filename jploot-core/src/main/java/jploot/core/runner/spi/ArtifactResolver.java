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
			List<Path> fragments = new ArrayList<>();
			fragments.add(config.jplootBase());
			fragments.add(Path.of(JPLOOT_BASE_ARTIFACTS_PATH));
			fragments.add(Path.of(String.format("%s-%s-%s.jar",
					application.groupId(),
					application.artifactId(),
					application.version())));
			
			Path path = fragments.stream().reduce((first, second) -> first.resolve(second)).get(); //NOSONAR
			try {
				pathHandler.isValidArtifact(path, application, config);
				artifactLookupBuilder.source(DependencySource.JPLOOT).path(path);
			} catch (JplootArtifactFailure failure) {
				artifactLookupBuilder.failure(failure);
			}
			artifactLookupsBuilder.putLookups(artifact, artifactLookupBuilder.build());
		}
		
		return artifactLookupsBuilder.build();
	}

}
