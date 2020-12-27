package jploot.cli;

import static com.pivovarit.function.ThrowingPredicate.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pivovarit.function.ThrowingSupplier;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import com.vdurmont.semver4j.SemverException;

import jploot.exceptions.InstallerException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "installer",
	mixinStandardHelpOptions = true,
	description = "Bootstrap jploot",
	hidden = true)
public class InstallerCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstallerCommand.class);

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
		
		Path targetJavaHome = target.resolve("jvm");
		if (javaHome != null && !ignoreJavaHomeEnvironment && !validateJavaHome(javaHome)) {
			return 1;
		} else if (javaHome != null && !ignoreJavaHomeEnvironment) {
			targetJavaHome = javaHome;
		}
		
		if (target.toFile().exists()) {
			LOGGER.warn("Target directory already exists. Jploot installation skipped");
		} else {
			installJploot(jplootHomeSource.get(), target);
			LOGGER.info("📌 Jploot installed ");
		}
		
		installJplootScripts(target, targetJavaHome, activate);
		
		LOGGER.info("📌 Jploot installed in {}", target);
		String activateCommand = String.format("source %s", target.resolve("bin/activate"));
		if (activate) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("⚡ Open a new terminal or load jploot with {}", activateCommand);
			}
		} else {
			LOGGER.info("⚡ Load jploot with {}", activateCommand);
		}
		LOGGER.info("⚡ Usage: jploot --help");
		return 0;
	}

	@Override
	boolean needsConfig() {
		return false;
	}

	private static void installJploot(Path jplootJavaHome, Path target) {
		try {
			if (target.toFile().exists()) {
				LOGGER.warn("Jploot JRE already exists, skipping install: {}", target);
			} else {
				createIfNotExists(target);
				copyDirectory(jplootJavaHome, target);
			}
		} catch (IOException e) {
			throw new InstallerException("Error installing Jploot", e);
		}
	}

	private static void installJplootScripts(Path jplootHome, Path javaHome, boolean activate) {
		File binDir = jplootHome.resolve("bin").toFile();
		if (!binDir.exists()) {
			binDir.mkdirs();
		}
		try {
			Path activateCommandPath = installActivation(jplootHome);
			if (activate) {
				installBashrcActivation(activateCommandPath);
			}
			installBinJploot(jplootHome, javaHome);
		} catch (IOException e) {
			throw new InstallerException("Failed to install jploot scripts", e);
		}
	}

	private static void installBinJploot(Path jplootHome, Path javaHome) throws IOException {
		Path jplootBin = jplootHome.resolve("bin/jploot");
		
		// create installer script
		String launcherScript;
		String resourcePath = "/jploot/scripts/jploot";
		launcherScript = readResourceToString(resourcePath);
		String classpathString = "\"$JPLOOT_HOME/jploot/*\"";
		launcherScript = launcherScript.replace("[[JAVA_HOME]]", javaHome.toString());
		launcherScript = launcherScript.replace("[[CLASSPATH]]", classpathString);
		launcherScript = launcherScript.replace("[[MAINCLASS]]", JplootMain.class.getName());
		
		String jplootBinContent = ThrowingSupplier.<String>lifted(() -> Files.readString(jplootBin)).get().orElse("");
		if (!launcherScript.equals(jplootBinContent)) {
			// install script and set execution permission
			Files.writeString(
					jplootBin,
					launcherScript,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			Files.setPosixFilePermissions(jplootBin, PosixFilePermissions.fromString("rwxr-xr-x"));
			LOGGER.debug("📌 Jploot runtime set to {}", jplootHome);
		} else {
			LOGGER.trace("📌 Jploot runtime already set to {}", jplootHome);
		}
	}

	private static void installBashrcActivation(Path activateCommandPath) throws IOException {
		Path bashrcFile = Path.of(System.getProperty("user.home"), ".bashrc");
		if (!bashrcFile.toFile().exists()) {
			LOGGER.warn("Bashrc file {} not found. Activation script is not installed", bashrcFile);
		} else {
			Pattern pattern = Pattern.compile("## jploot activation block\n.*\n## /jploot activation block", Pattern.DOTALL);
			String activation = "## jploot activation block\n"
					+ "JPLOOT_DISABLE_PROMPT=1\n"
					+ String.format("source %s\n", activateCommandPath)
					+ "## /jploot activation block";
			if (updateFile(bashrcFile, pattern, activation)) {
				LOGGER.debug("📌 Jploot activation script added to {}", bashrcFile);
			} else {
				LOGGER.trace("📌 Jploot activation script already present in {}", bashrcFile);
			}
		}
	}

	private static Path installActivation(Path jplootHome) throws IOException {
		Path activateCommandPath = jplootHome.resolve("bin/activate");
		String activateScript = readResourceToString("/jploot/scripts/activate");
		Files.writeString(
				activateCommandPath,
				activateScript,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		return activateCommandPath;
	}

	private static boolean updateFile(Path source, Pattern pattern, String replacement) throws IOException {
		String bashrcContent = Files.readString(source);
		String updatedContent;
		Matcher matcher = pattern.matcher(bashrcContent);
		if (matcher.find()) {
			updatedContent = matcher.replaceFirst(replacement);
		} else {
			updatedContent = bashrcContent + "\n" + replacement + "\n";
		}
		if (!bashrcContent.equals(updatedContent)) {
			Files.writeString(source, updatedContent, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			return true;
		} else {
			return false;
		}
	}

	private static String readResourceToString(String resourcePath) throws IOException {
		try (InputStream is = InstallerCommand.class.getResourceAsStream(resourcePath);
				InputStreamReader reader = new InputStreamReader(is);
				StringWriter writer = new StringWriter()) {
			reader.transferTo(writer);
			return writer.toString();
		}
	}

	private static boolean validateJavaHome(Path runtimeJavaHomePath) {
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

	private static String extractJavaVersionOutput(Path runtimeJavaHomePath) {
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

	private static boolean validateVersion(String output) {
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

	public static Optional<String> notBlankString(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		} else {
			return Optional.of(value);
		}
	}

	private static void copyDirectory(Path jplootHomePath, Path target) throws IOException {
		Path binDir = jplootHomePath.resolve("bin");
		copyFilesRecursively(jplootHomePath, target, binDir);
	}

	private static void copyFilesRecursively(Path rootFolder, Path targetFolder, Path... ignore) {
		Predicate<Path> isIgnoredFolder =
				d -> Arrays.stream(ignore).anyMatch(unchecked(i -> Files.isSameFile(d, i)));
		try {
			Files.walkFileTree(rootFolder, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (isIgnoredFolder.test(dir)) {
						return FileVisitResult.SKIP_SUBTREE;
					} else {
						copy(dir);
						return FileVisitResult.CONTINUE;
					}
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					copy(file);
					return FileVisitResult.CONTINUE;
				}
				
				private void copy(Path file) throws IOException {
					Files.copy(file, targetFolder.resolve(rootFolder.relativize(file)),
							StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
				}
				
				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					throw new InstallerException(
							String.format("Failed to copy %s to %s: %s", rootFolder, targetFolder, file), exc);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}});
		} catch (IOException e) {
			LOGGER.error(String.format("Error deleting temp directory %s", rootFolder), e);
		}
	}

	private static void createIfNotExists(Path target) {
		File file = target.toFile();
		if (!file.isDirectory()) {
			file.mkdirs();
		}
	}

}