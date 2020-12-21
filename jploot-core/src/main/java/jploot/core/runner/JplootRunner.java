package jploot.core.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.ArtifactLookup;
import jploot.config.model.ArtifactLookups;
import jploot.config.model.DependencySource;
import jploot.config.model.DependencyType;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.core.runner.spi.ArtifactResolver;
import jploot.core.runner.spi.PathHandler;

public class JplootRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootRunner.class);

	public void run(JplootConfig config, JplootApplication application, List<String> args) {
		run(config, application, args.toArray(new String[args.size()]));
	}

	public void run(JplootConfig config, JplootApplication application, String... args) {
		LOGGER.debug("Running {} in {}", config, application);
		// get default runtime
		JavaRuntime runtime = config.runtimes().iterator().next();
		// lookup jar
		ArtifactLookups lookups =
				new ArtifactResolver(new PathHandler()).resolve(config, application);
		if (lookups.failedLookups().count() == 0) {
			List<String> command = buildCommandLine(runtime, application, lookups, args);
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
				LOGGER.warn("{} cannot be resolved: {}", lookup.artifact(), lookup.failure().get().resolvedPath());
			}
			LOGGER.warn("Application {} cannot be launched as one or more dependencies are not satisfied", application);
		}
	}

	private List<String> buildCommandLine(JavaRuntime runtime, JplootApplication application, ArtifactLookups lookups,
			String... args) {
		// lookup java
		Path java = runtime.javaHome().resolve(Path.of("bin/java"));
		List<String> command = new ArrayList<>();
		command.add(java.toString());
		final boolean applicationInClasspath;
		if (!application.mainClass().isPresent()) {
			command.add("-jar");
			command.add(lookups.lookups().get(lookups.application()).path().get().toString());
			applicationInClasspath = false;
		} else {
			applicationInClasspath = true;
		}
		// extract classpath items
		Set<ArtifactLookup> classpath = lookups.lookups().entrySet().stream()
				.filter(i ->
							(applicationInClasspath || !lookups.application().equals(i.getKey()))
							&& i.getKey().types().contains(DependencyType.CLASSPATH)
							&& !DependencySource.JPLOOT_EMBEDDED.equals(i.getValue().source().get()))
				.map(Map.Entry::getValue)
				.collect(Collectors.toSet());
		if (!classpath.isEmpty()) {
			command.add("-classpath");
			command.add(classpath.stream().map(ArtifactLookup::path).map(Optional::get).map(Path::toString).collect(Collectors.joining(":")));
		}
		if (application.mainClass().isPresent()) {
			command.add(application.mainClass().get());
		}
		if (args != null) {
			command.addAll(Arrays.asList(args));
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Command built: {}", command.stream().collect(Collectors.joining(" ")));
		}
		return command;
	}

}
