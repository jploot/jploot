package jploot.config.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Load a file from a path ; abstraction used for:
 * <ul>
 *   <li>ease testing</li>
 *   <li>allow resource lookup from URI ?</li>
 * </ul>
 * </p>
 * 
 * <p>As whole content is loaded in a string, must be used only for low-weight files.</p>
 */
public class FileLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileLoader.class);

	public enum Mode {
		/**
		 * Empty files are replaced by {} content
		 */
		YAML;
	}

	public String load(Path location, Mode mode) {
		if (!location.toFile().exists()) {
			LOGGER.debug("Using an empty config as {} does not exist", location);
			return "{}";
		}
		try {
			String result = Files.readString(location);
			if (result.strip().isEmpty() && Mode.YAML.equals(mode)) {
				result = "{}";
			}
			return result;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
