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

import eu.mikroskeem.picomaven.DownloadResult;
import eu.mikroskeem.picomaven.PicoMaven;
import eu.mikroskeem.picomaven.artifact.Dependency;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;

public class JplootInstaller {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootInstaller.class);

	private static final URI MAVEN_CENTRAL_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");
	private static final URI JPLOOT_SNAPSHOTS_REPOSITORY = URI.create("http://localhost:8081/repository/jploot-snapshots/");
	private static final URI JPLOOT_RELEASES_REPOSITORY = URI.create("http://localhost:8081/repository/jploot-releases/");

	public void run(JplootConfig config, JplootApplication application) {
		Path temp = null;
		try {
			temp = Files.createTempDirectory(null);
			Path myTemp = temp;
			Path targetRepository = config.repository();
			LOGGER.debug("Running {} in {}", config, application);
			List<Dependency> dependencies = Arrays.<Dependency>asList(
					new Dependency(application.groupId(), application.artifactId(), application.version(),
							null, false /* no transitive lookup */, Collections.emptyList()));
			List<DownloadResult> downloaded = downloadDependencies(temp, dependencies);
			String jarUrl = String.format("jar:file://%s!%s", downloaded.get(0).getArtifactPath(), "/META-INF/jploot/jploot.properties");
			try {
				URL url = new URL(jarUrl);
				try (InputStream is = url.openConnection().getInputStream(); Reader reader = new InputStreamReader(is)) {
					Properties properties = new Properties();
					properties.load(reader);
					String dependenciesString = (String) properties.getOrDefault("classpathDependencies", "");
					List<Dependency> appDependencies = Arrays.stream(dependenciesString.split(" "))
							.map(String::strip)
							.map(JplootInstaller::dependencySpecToDependency)
							.collect(Collectors.toList());
					downloaded.addAll(downloadDependencies(myTemp, appDependencies));
				} catch (IOException e) {
					throw new RuntimeException(String.format("Failure reading property file %s", jarUrl), e);
				}
			} catch (MalformedURLException e) {
				throw new RuntimeException(String.format("URI format error for jar resource %s", jarUrl), e);
			}
			List<DownloadResult> failedDownloads = downloaded.stream()
					.filter(Predicate.not(DownloadResult::isSuccess))
					.collect(Collectors.toList());
			if (!failedDownloads.isEmpty()) {
				throw new RuntimeException(String.format("Dependency lookup failed on %s",
						String.join(" ", failedDownloads.stream().map(DownloadResult::toString).collect(Collectors.toList()))));
			}
			downloaded.stream()
				.filter(r -> r.getArtifactPath().getFileName().toString().endsWith(".jar"))
				.forEach(r -> {
					Path target = targetRepository.resolve(myTemp.relativize(r.getArtifactPath()));
					if (target.toFile().exists()) {
						return;
					}
					Path parent = target.getParent();
					if (!parent.toFile().isDirectory()) {
						parent.toFile().mkdirs();
					}
					try {
						Files.copy(r.getArtifactPath(), target);
					} catch (IOException e) {
						throw new RuntimeException(
								String.format("Error copying %s to %s", r.getArtifactPath(), target), e);
					}
				});
		} catch (IOException e1) {
			throw new RuntimeException("Error creating temporary file", e1);
		} finally {
			if (temp != null) {
				try {
					Files.walkFileTree(temp, new FileVisitor<Path>() {
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
					throw new RuntimeException(String.format("Error deleting temp directory %s", temp), e);
				}
			}
		}
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
			picoMaven.downloadAllArtifacts().values().stream().map(JplootInstaller::getFuture)
				.forEach(downloaded::add);
		}
		return downloaded;
	}

	private static DownloadResult getFuture(Future<DownloadResult> future) {
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
