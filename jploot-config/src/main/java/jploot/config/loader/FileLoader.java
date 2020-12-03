package jploot.config.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileLoader {

	public String load(Path location) {
		try {
			return Files.readString(location);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
