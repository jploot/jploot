package jploot.core.installer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pivovarit.function.ThrowingSupplier;
import com.pivovarit.function.exception.WrappedException;

import eu.mikroskeem.picomaven.DownloadResult;
import eu.mikroskeem.picomaven.PicoMaven;
import eu.mikroskeem.picomaven.artifact.Dependency;
import jploot.config.loader.JplootConfigManager;
import jploot.config.model.DependencySource;
import jploot.config.model.DependencyType;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootDependency;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.config.model.JplootDependency;

public class JplootInstaller {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootInstaller.class);

	private static final URI MAVEN_CENTRAL_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");
	private static final URI JPLOOT_SNAPSHOTS_REPOSITORY = URI.create("http://localhost:8081/repository/jploot-snapshots/");
	private static final URI JPLOOT_RELEASES_REPOSITORY = URI.create("http://localhost:8081/repository/jploot-releases/");

	private enum Step {
		TEMP_DIR,
		DOWNLOAD_APPLICATION,
		LOADING_APPLICATION_PROPERTIES,
		DOWNLOAD_APPLICATION_DEPENDENCIES,
		INSTALL_APPLICATION;
	}

	public void install(JplootConfig config, JplootConfigManager configManager, JplootDependency application) {
		Path targetRepository = config.repository();
		Step step = Step.TEMP_DIR;
		List<Runnable> finallyTasks = new ArrayList<>();
		try {
			Path temp = ThrowingSupplier.unchecked(() -> Files.createTempDirectory(null)).get();
			finallyTasks.add(() -> deleteFolderRecursively(temp));
			List<Dependency> dependencies = Arrays.asList(applicationToDependency(application));
			step = Step.DOWNLOAD_APPLICATION;
			List<DownloadResult> downloaded = downloadDependencies(temp, dependencies);
			step = Step.LOADING_APPLICATION_PROPERTIES;
			String jarUrl = String.format("jar:file://%s!%s", downloaded.get(0).getArtifactPath(), "/META-INF/jploot/jploot.properties");
			Properties properties = loadProperties(jarUrl);
			List<Dependency> appDependencies = collectDependencies(properties);
			step = Step.DOWNLOAD_APPLICATION_DEPENDENCIES;
			downloaded.addAll(downloadDependencies(temp, appDependencies));
			step = Step.INSTALL_APPLICATION;
			downloaded.stream()
				.filter(this::isJarFile)
				.forEach(r -> installArtifact(temp, targetRepository, r));
			List<JplootDependency> jplootDependencies = downloaded.stream()
					.map(DownloadResult::getDependency)
					.map(this::asJplootDependency)
					.collect(Collectors.toUnmodifiableList());
			JplootApplication installedApplication = ImmutableJplootApplication.builder()
					.name(properties.getProperty("applicationName"))
					.description(properties.getProperty("applicationDescription"))
					.mainClass(properties.getProperty("mainClass"))
					.groupId(application.groupId())
					.artifactId(application.artifactId())
					.version(application.version())
					.addAllAllowedSources(application.allowedSources())
					.addAllDependencies(jplootDependencies)
					.build();
			configManager.addApplication(config, installedApplication);
		} catch (RuntimeException e) {
			Throwable cause = e;
			if (e instanceof WrappedException) {
				cause = e.getCause();
			}
			switch (step) {
			case TEMP_DIR:
				throw new RuntimeException("Error creating temporary file", cause);
			case DOWNLOAD_APPLICATION:
				throw new RuntimeException("Download failed", cause);
			case LOADING_APPLICATION_PROPERTIES:
				throw new RuntimeException("Properties loading failed", cause);
			case DOWNLOAD_APPLICATION_DEPENDENCIES:
				throw new RuntimeException("Dependency download failed", cause);
			case INSTALL_APPLICATION:
				throw new RuntimeException("Install failed", cause);
			}
		} finally {
			for (Runnable finallyTask : finallyTasks) {
				// TODO: handle exception
				finallyTask.run();
			}
		}
	}

	private boolean isJarFile(DownloadResult downloadedArtifact) {
		return downloadedArtifact.getArtifactPath().getFileName().toString().endsWith(".jar");
	}

	private void installArtifact(Path rootFolder, Path targetRepository, DownloadResult downloadedArtifact) {
		Path target = targetRepository.resolve(rootFolder.relativize(downloadedArtifact.getArtifactPath()));
		if (target.toFile().exists()) {
			return;
		}
		Path parent = target.getParent();
		if (!parent.toFile().isDirectory()) {
			parent.toFile().mkdirs();
		}
		try {
			Files.copy(downloadedArtifact.getArtifactPath(), target);
			// add in jploot config
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("Error copying %s to %s", downloadedArtifact.getArtifactPath(), target), e);
		}
	}

	private Dependency applicationToDependency(JplootDependency application) {
		return new Dependency(application.groupId(), application.artifactId(), application.version(),
				null, false /* no transitive lookup */, Collections.emptyList());
	}

	private void deleteFolderRecursively(Path folder) {
		try {
			Files.walkFileTree(folder, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.deleteIfExists(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}});
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error deleting temp directory %s", folder), e);
		}
	}

	private Properties loadProperties(String jarUrl) {
		try {
			URL url = new URL(jarUrl);
			try (InputStream is = url.openConnection().getInputStream(); Reader reader = new InputStreamReader(is)) {
				Properties properties = new Properties();
				properties.load(reader);
				return properties;
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failure reading property file %s", jarUrl), e);
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(String.format("URI format error for jar resource %s", jarUrl), e);
		}
	}

	private JplootDependency asJplootDependency(Dependency dependency) {
		return ImmutableJplootDependency.builder()
				.groupId(dependency.getGroupId())
				.artifactId(dependency.getArtifactId())
				.version(dependency.getVersion())
				.addAllowedSources(DependencySource.JPLOOT)
				.addTypes(DependencyType.CLASSPATH)
				.build();
	}

	private List<Dependency> collectDependencies(Properties properties) {
		String dependenciesString = (String) properties.getOrDefault("classpathDependencies", "");
		return Arrays.stream(dependenciesString.split(" "))
				.map(String::strip)
				.map(JplootInstaller::dependencySpecToDependency)
				.collect(Collectors.toList());
	}

	private static Dependency dependencySpecToDependency(String dependencySpec) {
		String[] vars = (dependencySpec + " ").split(":");
		if (vars.length != 5) {
			throw new RuntimeException(String.format("Bad format for dependency spec: %s", dependencySpec));
		}
		return new Dependency(
				vars[0], vars[1], vars[2], // GAV
				// vars[3] TODO: type is ignored
				vars[4].isBlank() ? null : vars[4], // classifier
				false,
				Collections.emptyList()
		);
	}

	private List<DownloadResult> downloadDependencies(Path folder, List<Dependency> dependencies) {
		PicoMaven.Builder picoMavenBuilder = new PicoMaven.Builder()
				.withDownloadPath(folder)
				.withRepositories(Arrays.asList(
						MAVEN_CENTRAL_REPOSITORY,
						JPLOOT_RELEASES_REPOSITORY,
						JPLOOT_SNAPSHOTS_REPOSITORY))
				.withDependencies(dependencies);
		List<DownloadResult> downloaded = new ArrayList<>();
		try (PicoMaven picoMaven = picoMavenBuilder.build()) {
			picoMaven.downloadAllArtifacts().values().stream()
				.map(JplootInstaller::getDownloadResult)
				.forEach(downloaded::add);
		}
		List<DownloadResult> failedDownloads = downloaded.stream()
				.filter(Predicate.not(DownloadResult::isSuccess))
				.collect(Collectors.toList());
		if (!failedDownloads.isEmpty()) {
			List<String> failed = failedDownloads.stream().map(DownloadResult::toString).collect(Collectors.toList());
			throw new RuntimeException(
					String.format("Dependency lookup failed for %s",
					String.join(" ", failed)));
		}
		return downloaded;
	}

	private static DownloadResult getDownloadResult(Future<DownloadResult> future) {
		while (true) {
			try {
				return future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
