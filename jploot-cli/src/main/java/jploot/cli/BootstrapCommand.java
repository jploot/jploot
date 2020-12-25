package jploot.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import com.vdurmont.semver4j.SemverException;

import jploot.exceptions.BootstrapException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "bootstrap",
	mixinStandardHelpOptions = true,
	description = "Bootstrap jploot")
public class BootstrapCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapCommand.class);

	@Option(
			names = "--java-home",
			description = { "Provides your own JVM",
					"Path must be a valid JAVA_HOME value" }
			)
	Path javaHome;

	@Option(
			names = "--ignore-java-home",
			defaultValue = "false",
			description = { "Ignore JAVA_HOME environment variable" }
			)
	boolean ignoreJavaHomeEnvironment;

	@Parameters(index = "0..1",
			description = { "Target folder for jploot installation" },
			defaultValue = "${sys:user.home}/.local/share/jploot")
	Path target;

	@Option(
			names = "--activate",
			defaultValue = "true",
			description = { "Use --no-activate to disable bashrc activation" })
	boolean activate;

	@Override
	public Integer doCall() {
		Optional<Path> jplootHomeSource = notBlankString(System.getenv("JPLOOT_HOME")).map(Path::of);
		if (jplootHomeSource.isEmpty() || !jplootHomeSource.get().toFile().isDirectory()) {
			LOGGER.error("This jploot installation does not support bootstraping");
			LOGGER.error("Aborted");
			return 1;
		}
		
		Path targetJavaHome = jplootHomeSource.get().resolve("jvm");
		if (javaHome != null && !ignoreJavaHomeEnvironment && !validateJavaHome(javaHome)) {
			return 1;
		} else if (javaHome != null && !ignoreJavaHomeEnvironment) {
			targetJavaHome = javaHome;
		}
		
		if (target.toFile().exists()) {
			LOGGER.info("Target directory already exists. Jploot installation skipped");
		} else {
			installJploot(jplootHomeSource.get(), target);
			installJplootScripts(target, targetJavaHome, activate);
		}
		LOGGER.info("Jploot installed in {}", target);
		return 0;
	}

	@Override
	boolean needsConfig() {
		return false;
	}

	private void installJploot(Path jplootJavaHome, Path target) {
		try {
			if (target.toFile().exists()) {
				LOGGER.warn("Jploot JRE already exists, skipping install: {}", target);
			} else {
				createIfNotExists(target);
				copyDirectory(jplootJavaHome, target);
			}
		} catch (IOException e) {
			throw new BootstrapException("Error installing Jploot", e);
		}
	}

	private void installJplootScripts(Path jplootHome, Path javaHome, boolean activate) {
		try {
			Path activateCommandPath = jplootHome.resolve("bin/activate");
			String activateScript = readResourceToString("/jploot/scripts/activate");
			Files.writeString(
					activateCommandPath,
					activateScript,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			if (activate) {
				Path bashrcFile = Path.of(System.getProperty("user.home"), ".bashrc");
				if (!bashrcFile.toFile().exists()) {
					LOGGER.warn("Bashrc file {} not found. Activation script is not installed", bashrcFile);
				} else {
					Pattern pattern = Pattern.compile("## jploot activation block\n.*\n## /jploot activation block", Pattern.DOTALL);
					String activation = "## jploot activation block\n"
							+ String.format("source %s\n", activateCommandPath)
							+ "## /jploot activation block";
					updateFile(bashrcFile, pattern, activation);
				}
			}
			Path jplootBin = jplootHome.resolve("bin/jploot");
			updateFile(jplootBin, Pattern.compile("\nJAVA_HOME=[^\n]*\n"), String.format("\nJAVA_HOME='%s'\n", javaHome));
		} catch (IOException e) {
			throw new BootstrapException("Failed to install jploot scripts", e);
		}
	}

	private void updateFile(Path source, Pattern pattern, String replacement) throws IOException {
		String bashrcContent = Files.readString(source);
		Matcher matcher = pattern.matcher(bashrcContent);
		if (matcher.find()) {
			bashrcContent = matcher.replaceFirst(replacement);
		} else {
			bashrcContent += "\n" + replacement + "\n";
		}
		Files.writeString(source, bashrcContent, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}

	private static String readResourceToString(String resourcePath) throws IOException {
		try (InputStream is = BootstrapCommand.class.getResourceAsStream(resourcePath);
				InputStreamReader reader = new InputStreamReader(is);
				StringWriter writer = new StringWriter()) {
			reader.transferTo(writer);
			return writer.toString();
		}
	}

	private boolean validateJavaHome(Path runtimeJavaHomePath) {
		if (!runtimeJavaHomePath.toFile().isDirectory()
				|| !runtimeJavaHomePath.resolve("bin/java").toFile().isFile()) {
			LOGGER.error("Missing JVM environment: {}", runtimeJavaHomePath);
			return false;
		} else {
			String output = extractJavaVersionOutput(runtimeJavaHomePath);
			if (output == null) {
				return false;
			} else {
				return validateVersion(output);
			}
		}
	}

	private String extractJavaVersionOutput(Path runtimeJavaHomePath) {
		String output = null;
		List<String> command = List.of(runtimeJavaHomePath.resolve("bin/java").toString(), "-version");
		try {
			Process process = new ProcessBuilder(command).redirectOutput(Redirect.DISCARD)
				.redirectError(Redirect.PIPE)
				.start();
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				while (process.isAlive()) {
					process.getErrorStream().transferTo(outputStream);
				}
				int status = process.waitFor();
				if (status != 0) {
					LOGGER.error("Error running JVM {}", runtimeJavaHomePath);
				}
				output = outputStream.toString();
			} catch (InterruptedException e1) {
				LOGGER.error("Process interrupted while checking JVM version");
				Thread.currentThread().interrupt();
			}
		} catch (IOException e) {
			LOGGER.error("Error running JVM {}", runtimeJavaHomePath, e);
		}
		return output;
	}

	private boolean validateVersion(String output) {
		Optional<String> rawVersion = output.lines()
				.filter(l -> l.contains("openjdk version"))
				.findFirst()
				.<String>map(l -> Pattern.compile("(?<=\")[^\"]+(?=\")")
						.matcher(l).results()
						.findFirst().map(MatchResult::group).orElse(null));
		Optional<Semver> version;
		Throwable versionException = null;
		try {
			version = rawVersion.map(v -> new Semver(v.replace("_", "+"), SemverType.LOOSE));
		} catch (SemverException e) {
			version = Optional.empty();
			versionException = e;
		}
		if (version.isPresent() && version.get().isGreaterThan(new Semver("11.0.0"))) {
			return true;
		} else {
			if (rawVersion.isPresent()) {
				if (version.isEmpty()) {
					LOGGER.error("JVM version cannot be parsed: {}", rawVersion.get(), versionException);
				} else {
					LOGGER.error("JVM version does not match requirement: {} < 11", rawVersion.get());
				}
			} else {
				LOGGER.error("JVM version cannot be extracted: {}", output);
			}
			return false;
		}
	}

	public Optional<String> notBlankString(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		} else {
			return Optional.of(value);
		}
	}

	private void copyDirectory(Path javaHomePath, Path target) throws IOException {
		try (Stream<Path> walkStream = Files.walk(javaHomePath)) {
			walkStream.forEachOrdered(p -> {
				Path t = target.resolve(javaHomePath.relativize(p));
				LOGGER.trace("Copy: handling {} to {}", p, t);
				try {
					if (".".equals(p.getFileName().toString())
							|| "..".equals(p.getFileName().toString())) {
						return;
					}
					if (t.toFile().exists() && p.toFile().isDirectory()) {
						// already existing dirs cannot be removed
						return;
					}
					Files.copy(p, t, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new IllegalStateException(
							String.format("Error copying %s to %s", p, t), e);
				}
			});
		}
	}

	private void createIfNotExists(Path target) {
		File file = target.toFile();
		if (!file.isDirectory()) {
			file.mkdirs();
		}
	}

}