package jploot.api;

import java.nio.file.Path;

public interface IFileLoader {

	public enum Mode {
		/**
		 * Empty files are replaced by {} content
		 */
		YAML;
	}

	String load(Path location, Mode mode);

}