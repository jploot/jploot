package jploot.core.runner.spi;

import java.nio.file.Path;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootArtifact;
import jploot.config.model.JplootConfig;
import jploot.exceptions.ArtifactFailureType;
import jploot.exceptions.JplootArtifactFailure;

public class PathHandler {

	public void isValidArtifact(Path file, JplootApplication application, JplootArtifact artifact, JplootConfig config)
			throws JplootArtifactFailure {
		if (!file.toFile().exists()) {
			throw new JplootArtifactFailure(ArtifactFailureType.NOT_FOUND, file, application, artifact, config);
		} else if (!file.toFile().isFile()) {
			throw new JplootArtifactFailure(ArtifactFailureType.WRONG_TYPE, file, application, artifact, config);
		}
	}

}
