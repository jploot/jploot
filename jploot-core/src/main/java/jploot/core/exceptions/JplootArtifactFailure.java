package jploot.core.exceptions;

import java.nio.file.Path;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootBase;
import jploot.config.model.JplootConfig;

public class JplootArtifactFailure extends AbstractJplootApplicationException {

	private static final long serialVersionUID = -255974314767686716L;

	private final ArtifactFailureType type;
	private final Path resolvedPath;

	public JplootArtifactFailure(ArtifactFailureType type, Path resolvedPath,
			JplootApplication application, JplootConfig config, JplootBase jplootBase) {
		super(String.format("%s in %s not found: %s - %s", application, config, resolvedPath, type),
				application, config, jplootBase, null);
		this.type = type;
		this.resolvedPath = resolvedPath;
	}

}
