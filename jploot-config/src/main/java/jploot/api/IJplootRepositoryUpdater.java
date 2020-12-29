package jploot.api;

import java.nio.file.Path;

import jploot.config.model.JplootDependency;

public interface IJplootRepositoryUpdater {

	public enum InstallResult {
		INSTALLED,
		ALREADY_INSTALLED,
		UPDATED,
		REPLACED;
	}

	InstallResult install(JplootDependency dependency, Path artifact);

}
