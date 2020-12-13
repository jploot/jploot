package jploot.config.exceptions;

import java.nio.file.Path;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootArtifact;
import jploot.config.model.JplootConfig;

public class JplootArtifactFailure extends AbstractJplootApplicationException {

	private static final long serialVersionUID = -255974314767686716L;

	private final ArtifactFailureType type;
	private final Path resolvedPath;

	public JplootArtifactFailure(ArtifactFailureType type, Path resolvedPath,
			JplootApplication application, JplootArtifact artifact, JplootConfig config) {
		super(String.format("%s/%s in %s not found: %s - %s", application, artifact, config, resolvedPath, type),
				application, config, null);
		this.type = type;
		this.resolvedPath = resolvedPath;
	}

	public Path resolvedPath() {
		return resolvedPath;
	}

}
