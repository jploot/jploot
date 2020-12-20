package jploot.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import jploot.maven.impl.Jdk;

@Mojo(name = "jploot", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresOnline = true, requiresProject = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class JplootMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.directory}/jploot/", required = true)
	private String outputDirectory;

	@Parameter(required = true)
	private String mainClass;

	@Parameter(required = true)
	private String scriptName;

	@Parameter
	private List<String> modules;

	@Parameter
	private List<String> options;

	@Parameter
	private MavenSession mavenSession;

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Path outputDirectoryPath = Path.of(outputDirectory);
			
			if (!outputDirectoryPath.toFile().exists()) {
				outputDirectoryPath.toFile().mkdirs();
			}
			
			Jdk jdk = resolveJdk();
			if (!jdk.isFound()) {
				String message = String.format("Expected JDK %s is missing", jdk.javaHome());
				getLog().error(message);
				throw new MojoFailureException(message);
			}
			
			Path jreDirectory = jreDirectory(outputDirectoryPath);
			buildJre(jdk, jreDirectory);
			stripJre(jreDirectory);
			addApplication(jreDirectory, scriptName);
			
			List<String> makeselfCommand = new ArrayList<>();
			Path target = outputDirectoryPath.resolve(scriptName);
			
			makeselfCommand.add("makeself");
			makeselfCommand.add(jreDirectory.toAbsolutePath().toString());
			makeselfCommand.add(target.toAbsolutePath().toString());
			makeselfCommand.add(project.getArtifactId());
			makeselfCommand.add(Path.of("bin", scriptName).toString());
			runCommand(getLog(), makeselfCommand);
		} catch (InterruptedException | IOException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new MojoExecutionException("Unexpected failure", e);
		}
	}

	private void addApplication(Path jreDirectory, String scriptName) throws IOException {
		Path targetApplication = jreDirectory.resolve("application");
		File targetApplicationFile = targetApplication.toFile();
		if (!targetApplicationFile.exists()) {
			targetApplicationFile.mkdirs();
		}
		getLog().info("Installed application artifacts:");
		List<String> classpath = new ArrayList<>();
		for (Artifact artifact : project.getArtifacts()) {
			if (artifact.getArtifactHandler().isAddedToClasspath()) {
				File file = artifact.getFile();
				Path target = targetApplication.resolve(file.getName());
				getLog().debug(String.format("copy %s to %s", file.getAbsolutePath(), target));
				Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
				classpath.add(file.getName());
				getLog().info(String.format("%s (%s)", file.getName(), artifact));
			} else {
				getLog().debug(String.format("Ignored artifact: %s", artifact));
			}
		}
		String launcherScript;
		String resourcePath = "/jploot-installer/launcher";
		launcherScript = readResourceToString(resourcePath);
		launcherScript = launcherScript.replace(
				"[[CLASSPATH]]",
				classpath.stream().map(s -> "\"$JAVA_HOME\"/application/" + escape(s)).collect(Collectors.joining(":"))
				);
		launcherScript = launcherScript.replace(
				"[[MAINCLASS]]",
				mainClass
				);
		Path launcher = jreDirectory.resolve("bin").resolve(scriptName);
		Files.writeString(
				launcher,
				launcherScript,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		Files.setPosixFilePermissions(launcher, PosixFilePermissions.fromString("rwxr-xr-x"));
		getLog().info("JRE launcher script added");
	}

	private void stripJre(Path jreDirectory) throws IOException, InterruptedException, MojoFailureException {
		List<String> stripCommand = new ArrayList<>();
		stripCommand.add("strip");
		stripCommand.add("-p");
		stripCommand.add("--strip-unneeded");
		Files.walk(jreDirectory.resolve("lib"))
			.filter(p -> p.toFile().isFile() && p.getFileName().toString().endsWith(".so"))
			.map(Path::toAbsolutePath)
			.map(Path::toString)
			.forEach(stripCommand::add);
		runCommand(getLog(), stripCommand);
		
		getLog().info("JRE stripped down");
	}

	private void buildJre(Jdk jdk, Path jreDirectory) throws IOException, InterruptedException, MojoFailureException {
		// TODO: use a better condition
		if (jreDirectory.toFile().exists()) {
			getLog().info(String.format("JRE already present: %s", jreDirectory));
			return;
		}
		if (jreDirectory.toFile().exists()) {
			getLog().info(String.format("Cleaning existing directory %s", jreDirectory));
			FileUtils.deleteDirectory(jreDirectory.toFile());
		}
		List<String> command = new ArrayList<>();
		command.add(jdk.jlink().toAbsolutePath().toString());
		command.add("-v");
		if (options != null) {
			options.stream().forEach(command::add);
		}
		command.add("--module-path");
		command.add(jdk.jmods().toAbsolutePath().toString());
		// TODO: which module to include; empty modules is not an option
		if (modules != null) {
			command.add("--add-modules");
			command.add(String.join(",", modules));
		}
		command.add("--output");
		command.add(jreDirectory.toAbsolutePath().toString());
		runCommand(getLog(), command);
		
		Path targetJava = jreDirectory.resolve("bin").resolve("java");
		if (!validate(targetJava, JplootMojo::isExecutableFile)) {
			String message = String.format(
					"Unexpected missing executable file %s after a successful jlink building",
					targetJava);
			getLog().error(message);
			throw new MojoFailureException(message);
		}
		
		getLog().info(String.format("JRE generated in %s", jreDirectory));
	}

	private static String readResourceToString(String resourcePath) throws IOException {
		try (InputStream is = JplootMojo.class.getResourceAsStream(resourcePath)) {
			return IOUtil.toString(is);
		}
	}

	private void runCommand(Log log, List<String> command) throws IOException, InterruptedException, MojoFailureException {
		String commandString = commandAsString(command);
		
		log.info(String.format("Executing %s", commandString));
		
		Process p = new ProcessBuilder(command).inheritIO().start();
		int result = p.waitFor();
		if (result != 0) {
			String message = String.format("Command %s failed with status %d",
					commandString,
					result);
			log.error(message);
			throw new MojoFailureException(message);
		}
	}

	private static String commandAsString(List<String> command) {
		return command.stream().map(JplootMojo::escape).collect(Collectors.joining(" "));
	}

	private static String escape(String unescaped) {
		return "'" + unescaped.replace("'", "'\"'\"'") + "'";
	}

	private static Path jreDirectory(Path outputDirectory) {
		return outputDirectory.resolve("jre");
	}

	private static Jdk resolveJdk() {
		String javaHome = System.getProperty("java.home", null);
		if (javaHome == null) {
			return new Jdk(null, false);
		}
		Path javaHomePath = Path.of(javaHome);
		Jdk jdk = new Jdk(javaHomePath, true);
		if (validate(jdk.javaHome(), File::isDirectory)
				&& validate(jdk.java(), JplootMojo::isExecutableFile)
				&& validate(jdk.jlink(), JplootMojo::isExecutableFile)) {
			return jdk;
		} else {
			return new Jdk(javaHomePath, false);
		}
	}

	private static boolean validate(Path path, Predicate<File> conditions) {
		File file = path.toFile();
		return conditions.test(file);
	}

	private static boolean isExecutableFile(File file) {
		if (!file.isFile()) {
			return false;
		}
		if (!file.canExecute()) {
			return false;
		}
		return true;
	}
}
