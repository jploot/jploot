package jploot.core.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.ArtifactLookup;
import jploot.config.model.ArtifactLookups;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootArtifact;
import jploot.config.model.JplootConfig;
import jploot.core.runner.spi.ArtifactResolver;
import jploot.core.runner.spi.PathHandler;
import jploot.exceptions.JplootArtifactFailure;
import jploot.exceptions.RunException;

public class JplootRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootRunner.class);

	public void run(JplootConfig config, JplootApplication application, List<String> args) {
		run(config, application, args.toArray(new String[args.size()]));
	}

	public void run(JplootConfig config, JplootApplication application, String... args) {
		// get default runtime
		JavaRuntime runtime = config.runtimes().iterator().next();
		// lookup jar
		LOGGER.debug("⏳ Application's dependencies lookup");
		ArtifactLookups lookups =
				new ArtifactResolver(new PathHandler()).resolve(config, application);
		if (lookups.failedLookups().count() == 0) {
			LOGGER.info("👌 Application's dependencies lookup");
			List<String> command = buildCommandLine(runtime, application, lookups, args);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("⚡ Running {}", command.stream().collect(CommandLineCollector.INSTANCE));
			}
			try {
				Process p = new ProcessBuilder(command).inheritIO().start();
				int status = p.waitFor();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("🔚 Application exit code: {}", status);
				}
			} catch (IOException e) {
				throw new RunException(String.format("Application %s cannot be launched", application), e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RunException("Interrupted during execution", e);
			}
		} else {
			for (ArtifactLookup lookup : lookups.failedLookups().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).values()) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("{} cannot be resolved: {}",
							lookup.artifact().asSpec(),
							lookup.failure()
								.map(JplootArtifactFailure::resolvedPath)
								.map(Path::toString).orElse("<no path>"));
				}
			}
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Application {} cannot be launched as one or more dependencies are not satisfied", application.asSpec());
			}
		}
	}

	private List<String> buildCommandLine(JavaRuntime runtime, JplootApplication application, ArtifactLookups lookups,
			String... args) {
		// lookup java
		Path java = runtime.javaHome().resolve(Path.of("bin/java"));
		LOGGER.trace("🔍 Using java binary {}", java);
		List<String> command = new ArrayList<>();
		command.add(java.toString());
		final boolean applicationInClasspath;
		Optional<String> mainClass = application.mainClass();
		if (!mainClass.isPresent()) {
			LOGGER.trace("⭕ No mainClass; use -jar option and MANIFEST entry-point");
			command.add("-jar");
			command.add(lookups.lookups().get(lookups.application()).resolvedPath().toString());
			applicationInClasspath = false;
		} else {
			applicationInClasspath = true;
		}
		// extract classpath items
		Set<ArtifactLookup> classpath = lookups.lookups().values().stream()
				.filter(i -> isClasspathDependency(applicationInClasspath ? null : lookups.application(), i))
				.collect(Collectors.toSet());
		if (!classpath.isEmpty()) {
			command.add("-classpath");
			command.add(classpath.stream().map(ArtifactLookup::path).map(Optional::get).map(Path::toString).collect(Collectors.joining(":")));
		}
		if (mainClass.isPresent()) {
			command.add(mainClass.get());
		}
		if (args != null) {
			command.addAll(Arrays.asList(args));
		}
		return command;
	}

	private boolean isClasspathDependency(JplootApplication excludedApplication, ArtifactLookup lookup) {
		JplootArtifact artifact = lookup.artifact();
		return (excludedApplication == null || !artifact.equals(excludedApplication));
	}

	static class CommandLineCollector implements Collector<String, StringBuilder, String> {

		static final CommandLineCollector INSTANCE = new CommandLineCollector();

		@Override
		public Supplier<StringBuilder> supplier() {
			return StringBuilder::new;
		}

		@Override
		public BiConsumer<StringBuilder, String> accumulator() {
			return (sb, s) -> { sb.append(" "); sb.append(escape(s)); };
		}

		@Override
		public BinaryOperator<StringBuilder> combiner() {
			return (sb1, sb2) -> { sb1.append(" "); sb1.append(sb2.toString()); return sb1; };
		}

		@Override
		public Function<StringBuilder, String> finisher() {
			return StringBuilder::toString;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Collections.emptySet();
		}

		private String escape(String arg) {
			if (arg.matches("[a-zA-Z0-9,._+:@%/-]*")) {
				// safe chars
				return arg;
			} else if (arg.matches("[a-zA-Z0-9,._+:@%/-']*")) {
				return "\"" + arg + "\"";
			} else {
				return "'" + arg.replace("'", "'\"'\"'") + "'";
			}
		}
	}

}
