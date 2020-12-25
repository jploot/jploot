package jploot.config.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jploot.api.IJplootRepositoryUpdater;
import jploot.config.model.JplootConfig;
import jploot.config.model.JplootDependency;
import jploot.exceptions.InstallException;

public class JplootRepositoryUpdater extends AbstractJplootConfigHandling implements IJplootRepositoryUpdater {

	JplootConfig config;

	public JplootRepositoryUpdater(FileLoader fileLoader, JplootConfig config) {
		super(fileLoader);
		this.config = config;
	}

	@Override
	public InstallResult install(JplootDependency dependency, Path artifact) {
		Path targetRepository = config.repository();
		List<String> path = new ArrayList<>();
		Collections.addAll(path, dependency.groupId().split("\\."));
		path.add(String.format("%s-%s.%s", dependency.artifactId(), dependency.version(), "jar"));
		Path target = path.stream().<Path>reduce(
				targetRepository,
				(p, c) -> p.resolve(c), (p, c) -> p.resolve(c));
		if (target.toFile().exists()) {
			return InstallResult.ALREADY_INSTALLED;
		}
		Path parent = target.getParent();
		if (!parent.toFile().isDirectory() && !parent.toFile().mkdirs()) {
			throw new InstallException(String.format("Failed to create %s", parent));
		}
		try {
			Files.copy(artifact, target);
			return InstallResult.INSTALLED;
			// TODO: add in jploot config
		} catch (IOException e) {
			throw new InstallException(String.format("Error installing %s in %s", dependency, config), e);
		}
	}
}
