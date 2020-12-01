package jploot.core.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.ArtifactLookup;
import jploot.config.model.ArtifactLookups;
import jploot.config.model.DependencyType;
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
		ArtifactLookups lookups =
				new ArtifactResolver(new PathHandler()).resolve(config, jplootBase, application);
		if (lookups.failedLookups().count() == 0) {
			List<String> command = buildCommandLine(runtime, lookups);
			try {
				Process p = new ProcessBuilder(command).inheritIO().start();
				int status = p.waitFor();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Application {} exit code: {}", application, status);
				}
			} catch (IOException e) {
				throw new IllegalStateException(String.format("Application %s cannot be launched", application), e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(
						String.format("Interrupted during application %s run", application), e);
			}
		} else {
			for (ArtifactLookup lookup : lookups.failedLookups().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).values()) {
				LOGGER.warn("{} cannot be resolved", lookup.artifact());
			}
			LOGGER.warn("Application {} cannot be launched as one or more dependencies are not satisfied", application);
		}
	}

	private List<String> buildCommandLine(JavaRuntime runtime, ArtifactLookups lookups) {
		// lookup java
		Path java = Path.of("bin/java").resolve(runtime.javaHome());
		List<String> command = new ArrayList<>();
		command.add(java.toString());
		command.add("-jar");
		command.add(lookups.lookups().get(lookups.application()).path().toString());
		// extract classpath items
		Set<ArtifactLookup> classpath = lookups.lookups().entrySet().stream()
				.filter(i ->
							!lookups.application().equals(i.getKey())
							&& i.getKey().types().contains(DependencyType.CLASSPATH))
				.map(Map.Entry::getValue)
				.collect(Collectors.toSet());
		if (!classpath.isEmpty()) {
			command.add("-classpath");
			classpath.stream().forEach(i -> { command.add(i.path().toString()); });
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Command built: {}", command.stream().collect(Collectors.joining(" ")));
		}
		return command;
	}

}
