package jploot.core.runner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.exceptions.JplootArtifactFailure;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootBase;
import jploot.config.model.JplootConfig;
import jploot.core.runner.spi.ArtifactResolver;
import jploot.core.runner.spi.PathHandler;

public class JplootRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootRunner.class);

	public void run(JplootConfig config, JplootBase jplootBase, JplootApplication application) {
		LOGGER.debug("Running {} in {}", jplootBase, application);
		// get default runtime
		JavaRuntime runtime = jplootBase.javaRuntimes().get(0);
		// lookup jar
		try {
			Path jar = new ArtifactResolver(new PathHandler()).resolve(config, jplootBase, application);
			// lookup java
			Path java = Path.of("bin/java").resolve(runtime.javaHome());
			List<String> command = new ArrayList<>();
			command.add(java.toString());
			command.add("-jar");
			command.add(jar.toString());
			LOGGER.debug("Command built: {}", command.stream().collect(Collectors.joining(" ")));
		} catch (JplootArtifactFailure e) {
			// TODO
		}
	}

}
