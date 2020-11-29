package jploot.core.runner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootBase;

public class JplootArtifactResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootArtifactResolver.class);

	public JplootArtifactResolver() {
		super();
	}

	public Path resolve(JplootBase jplootBase, JplootApplication application) {
		LOGGER.debug("Resolving {} in {}", application, jplootBase);
		
		List<Path> fragments = new ArrayList<>();
		fragments.add(jplootBase.location());
		fragments.add(Path.of(String.format("%s-%s-%s.jar",
				application.groupId(),
				application.artifactId(),
				application.version())));
		
		Optional<Path> path = fragments.stream().reduce((first, second) -> second.resolve(first));
		
		if (path.isPresent() && path.get().toFile().exists() && path.get().toFile().isFile()) {
			return path.get();
		} else if (path.isPresent() && path.get().toFile().exists()) {
			throw new IllegalStateException(String.format("%s resolved in %s to non-file %s",
					jplootBase, application, path.get()));
		} else {
			throw new IllegalStateException(String.format("%s cannot be resolved in %s", jplootBase, application));
		}
	}

}
