package jploot.core.runner.spi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootBase;
import jploot.config.model.JplootConfig;
import jploot.core.exceptions.JplootArtifactFailure;

public class ArtifactResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactResolver.class);

	private final PathHandler pathHandler;

	public ArtifactResolver(PathHandler pathHandler) {
		super();
		this.pathHandler = pathHandler;
	}

	public Path resolve(JplootConfig config, JplootBase jplootBase, JplootApplication application)
			throws JplootArtifactFailure {
		LOGGER.debug("Resolving {} in {}", application, jplootBase);
		
		List<Path> fragments = new ArrayList<>();
		fragments.add(jplootBase.location());
		fragments.add(Path.of(String.format("%s-%s-%s.jar",
				application.groupId(),
				application.artifactId(),
				application.version())));
		
		Path path = fragments.stream().reduce((first, second) -> first.resolve(second)).get(); //NOSONAR
		
		pathHandler.isValidArtifact(path, application, config, jplootBase);
		
		return path;
	}

}
