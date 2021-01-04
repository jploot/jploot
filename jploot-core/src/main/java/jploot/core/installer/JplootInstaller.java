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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.pivovarit.function.ThrowingSupplier;
import com.pivovarit.function.exception.WrappedException;

import eu.mikroskeem.picomaven.DownloadResult;
import eu.mikroskeem.picomaven.PicoMaven;
import eu.mikroskeem.picomaven.artifact.Dependency;
import jploot.api.IJplootConfigUpdater;
import jploot.api.IJplootLauncherManager;
import jploot.api.IJplootRepositoryUpdater;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootDependency;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootDependency;
import jploot.exceptions.InstallException;

public class JplootInstaller {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootInstaller.class);

	private enum Step {
		TEMP_DIR,
		DOWNLOAD_APPLICATION,
		LOADING_APPLICATION_PROPERTIES,
		DOWNLOAD_APPLICATION_DEPENDENCIES,
		INSTALL_DEPENDENCIES,
		INSTALL_APPLICATION;
	}

	public JplootApplication install(
			Set<JplootApplication> installedApplications,
			List<URI> repositories,
			IJplootConfigUpdater configUpdater,
			IJplootRepositoryUpdater repositoryUpdater,
			IJplootLauncherManager launcherManager,
			JplootDependency application) {
		Step step = Step.TEMP_DIR;
		List<Runnable> finallyTasks = new ArrayList<>();
		try {
			Path temp = ThrowingSupplier.unchecked(() -> Files.createTempDirectory(null)).get();
			finallyTasks.add(() -> deleteFolderRecursively(temp));
			step = Step.DOWNLOAD_APPLICATION;
			LOGGER.debug("⏳ Application download");
			ImmutableBiMap.Builder<Dependency, DependencyResult> lookupsBuilder = ImmutableBiMap.builder();
			DependencyResult applicationResult = applicationToDependency(application);
			List<DependencyResult> dependencies = Arrays.asList(applicationResult);
			dependencies.stream().forEach(d -> lookupsBuilder.put(d.dependency, d));
			ImmutableBiMap<Dependency, DependencyResult> applicationLookup = lookupsBuilder.build();
			downloadDependencies(repositories, temp, applicationLookup);
			LOGGER.info("🌐 Application downloaded");
			step = Step.LOADING_APPLICATION_PROPERTIES;
			// dependency is successful as downloadDependencies throws exception on download failure
			Path applicationArtifactJar = applicationLookup.values().stream() //NOSONAR
					.findFirst().get().downloadResult.getArtifactPath();
			String jarUrl = String.format("jar:file://%s!%s", applicationArtifactJar, "/META-INF/jploot/jploot.properties");
			Properties properties = loadProperties(jarUrl);
			BiMap<Dependency, DependencyResult> dependenciesLookup = collectDependencies(properties);
			LOGGER.info("🔍 Application's dependencies lookup");
			step = Step.DOWNLOAD_APPLICATION_DEPENDENCIES;
			LOGGER.debug("⏳ Application's dependencies download");
			downloadDependencies(repositories, temp, dependenciesLookup);
			LOGGER.info("🌐 Application's dependencies download");
			step = Step.INSTALL_DEPENDENCIES;
			LOGGER.debug("⏳ Application's dependencies installation");
			dependenciesLookup.values().stream()
				.filter(this::isJarFile)
				.forEach(d -> installArtifact(repositoryUpdater, d));
			List<JplootDependency> jplootDependencies = dependenciesLookup.values().stream()
					.map(d -> d.jplootDependency)
					.collect(Collectors.toUnmodifiableList());
			Set<String> launcherCandidates = new HashSet<String>();
			Set<String> launchers = new HashSet<String>();
			String applicationName = properties.getProperty("applicationName");
			launcherCandidates.add(String.format("%s-%s", applicationName, application.version()));
			launcherCandidates.add(String.format("%s", applicationName));
			for (String candidate : launcherCandidates) {
				if (installedApplications.stream()
						.noneMatch(a -> a.launchers().map(l -> l.contains(candidate)).orElse(false))) {
					launchers.add(candidate);
				}
			}
			JplootApplication installedApplication = ImmutableJplootApplication.builder()
					.name(applicationName)
					.description(properties.getProperty("applicationDescription"))
					.mainClass(properties.getProperty("mainClass"))
					.groupId(application.groupId())
					.artifactId(application.artifactId())
					.version(application.version())
					.addAllDependencies(jplootDependencies)
					.launchers(launchers)
					.build();
			LOGGER.info("📌 Application's dependencies installation");
			step = Step.INSTALL_APPLICATION;
			LOGGER.debug("⏳ Application installation");
			installArtifact(repositoryUpdater, applicationResult);
			installLaunchers(launcherManager, installedApplication);
			configUpdater.addApplication(installedApplication);
			LOGGER.info("📌 Application installation");
			return installedApplication;
		} catch (RuntimeException e) {
			Throwable cause = e;
			if (e instanceof WrappedException) {
				cause = e.getCause();
			}
			switch (step) {
			case TEMP_DIR:
				throw new InstallException("Error creating temporary file", cause);
			case DOWNLOAD_APPLICATION:
				throw new InstallException("Download failed", cause);
			case LOADING_APPLICATION_PROPERTIES:
				throw new InstallException("Properties loading failed", cause);
			case DOWNLOAD_APPLICATION_DEPENDENCIES:
				throw new InstallException("Dependency download failed", cause);
			case INSTALL_DEPENDENCIES:
				throw new InstallException("Dependency install failed", cause);
			case INSTALL_APPLICATION:
			default:
				throw new InstallException("Install failed", cause);
			}
		} finally {
			for (Runnable finallyTask : finallyTasks) {
				// TODO: handle exception
				finallyTask.run();
			}
		}
	}

	private boolean isJarFile(DependencyResult lookup) {
		return lookup.downloadResult.getArtifactPath().getFileName().toString().endsWith(".jar");
	}

	private void installLaunchers(IJplootLauncherManager launcherManager, JplootApplication application) {
		launcherManager.addLaunchers(application);
	}

	private void installArtifact(IJplootRepositoryUpdater updater,
			DependencyResult dependency) {
		updater.install(dependency.jplootDependency, dependency.downloadResult.getArtifactPath());
	}

	private DependencyResult applicationToDependency(JplootDependency jplootDependency) {
		Dependency dependency = new Dependency(
				jplootDependency.groupId(), jplootDependency.artifactId(), jplootDependency.version(),
				null /* classifier */, false /* no transitive lookup */, Collections.emptyList() /* checkums */);
		return new DependencyResult(dependency, jplootDependency);
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
			LOGGER.error(String.format("Error deleting temp directory %s", folder), e);
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
				throw new InstallException(String.format("Failure reading property file %s", jarUrl), e);
			}
		} catch (MalformedURLException e) {
			throw new InstallException(String.format("URI format error for jar resource %s", jarUrl), e);
		}
	}

	private BiMap<Dependency, DependencyResult> collectDependencies(Properties properties) {
		ImmutableBiMap.Builder<Dependency, DependencyResult> builder = ImmutableBiMap.builder();
		String dependenciesString = (String) properties.getOrDefault("classpathDependencies", "");
		Arrays.stream(dependenciesString.split(" "))
				.map(String::strip)
				.map(JplootInstaller::dependencySpecToDependency)
				.map(this::applicationToDependency)
				.forEach(d -> builder.put(d.dependency, d));
		return builder.build();
	}

	private static JplootDependency dependencySpecToDependency(String dependencySpec) {
		String[] vars = (dependencySpec + " ").split(":");
		if (vars.length != 5) {
			throw new RuntimeException(String.format("Bad format for dependency spec: %s", dependencySpec));
		}
		return ImmutableJplootDependency.builder()
				.groupId(vars[0]).artifactId(vars[1]).version(vars[2]) // GAV
				.build();
	}

	private void downloadDependencies(List<URI> repositories,
			Path folder, BiMap<Dependency, DependencyResult> dependencies) {
		List<Dependency> dependenciesList = new ArrayList<>(dependencies.keySet());
		PicoMaven.Builder picoMavenBuilder = new PicoMaven.Builder()
				.withDownloadPath(folder)
				.withRepositories(repositories)
				.withDependencies(dependenciesList);
		try (PicoMaven picoMaven = picoMavenBuilder.build()) {
			picoMaven.downloadAllArtifacts().values().stream()
				.map(JplootInstaller::getDownloadResult)
				.forEach(d -> dependencies.get(d.getDependency()).downloadResult = d);
		}
		List<DependencyResult> failedDownloads = dependencies.values().stream()
				.filter(d -> (d.downloadResult == null || !d.downloadResult.isSuccess()))
				.collect(Collectors.toList());
		if (!failedDownloads.isEmpty()) {
			List<String> failed = failedDownloads.stream()
					.map(d -> d.downloadResult.toString())
					.collect(Collectors.toList());
			throw new InstallException(
					String.format("Dependency lookup failed for %s",
					String.join(" ", failed)));
		}
	}

	private static DownloadResult getDownloadResult(Future<DownloadResult> future) {
		while (true) {
			try {
				return future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				throw new InstallException(e);
			}
		}
	}

	private static class DependencyResult {
		DownloadResult downloadResult;
		JplootDependency jplootDependency;
		Dependency dependency;
		
		public DependencyResult(Dependency dependency, JplootDependency jplootDependency) {
			this.dependency = dependency;
			this.jplootDependency = jplootDependency;
		}
	}

}
