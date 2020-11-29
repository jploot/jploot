package jploot.core.runner.spi;

import java.nio.file.Path;

import jploot.config.exceptions.ArtifactFailureType;
import jploot.config.exceptions.JplootArtifactFailure;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootBase;
import jploot.config.model.JplootConfig;

public class PathHandler {

	public void isValidArtifact(Path file, JplootApplication application, JplootConfig config, JplootBase jplootBase)
			throws JplootArtifactFailure {
		if (!file.toFile().exists()) {
			throw new JplootArtifactFailure(ArtifactFailureType.NOT_FOUND, file, application, config, jplootBase);
		} else if (file.toFile().isFile()) {
			throw new JplootArtifactFailure(ArtifactFailureType.WRONG_TYPE, file, application, config, jplootBase);
		}
	}

}
