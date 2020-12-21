package jploot.config.loader;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import jploot.config.model.ArgumentConfig;
import jploot.config.model.DependencySource;
import jploot.config.model.DependencyType;
import jploot.config.model.ImmutableJavaRuntime;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootConfig;
import jploot.config.model.ImmutableJplootDependency;
import jploot.config.model.ImmutableMavenRepository;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.config.model.JplootDependency;
import jploot.config.model.MavenRepository;
import jploot.config.model.yaml.ImmutableJplootApplicationFile;
import jploot.config.model.yaml.ImmutableJplootConfigFile;
import jploot.config.model.yaml.ImmutableJplootDependencyFile;
import jploot.config.model.yaml.JavaRuntimeFile;
import jploot.config.model.yaml.JplootApplicationFile;
import jploot.config.model.yaml.JplootConfigFile;
import jploot.config.model.yaml.JplootDependencyFile;
import jploot.config.model.yaml.MavenRepositoryFile;

/**
 * <p>
 * Load a jploot config file from a {@link ArgumentConfig}. Default configuration are applied in case configuration
 * file is incomplete.
 * </p>
 */
public class JplootConfigManager {

	private final ObjectMapper mapper;
	private final FileLoader fileLoader;

	public JplootConfigManager(FileLoader fileLoader) {
		super();
		this.fileLoader = fileLoader;
		mapper = new ObjectMapper(new YAMLFactory());
		// handle Optional types
		mapper.registerModule(new Jdk8Module());
	}

	public JplootConfig load(ArgumentConfig config) {
		return load(config.location());
	}

	private JplootConfig load(Path location) {
		try {
			JplootConfigFile configFile = loadJplootConfigFile(location);
			ImmutableJplootConfig.Builder builder = ImmutableJplootConfig.builder()
					.location(location);
			builder.applications(applications(configFile));
			builder.runtimes(runtimes(configFile));
			builder.mavenRepositories(mavenRepositories(configFile));
			return builder.build();
		} catch (MissingConfigException e) {
			throw new IllegalStateException(e);
		}
	}

	private JplootConfigFile loadJplootConfigFile(Path location) {
		String content = fileLoader.load(location, FileLoader.Mode.YAML);
		try {
			return mapper.readValue(content, JplootConfigFile.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	public JplootConfig addApplication(JplootConfig config, JplootApplication application) {
		JplootConfigFile currentConfig = loadJplootConfigFile(config.location());
		Optional<Set<JplootApplicationFile>> currentApplications = currentConfig.applications();
		Set<JplootApplicationFile> newApplications = new HashSet<>();
		if (currentApplications.isPresent()) {
			newApplications.addAll(currentApplications.get());
		}
		newApplications.add(applicationFile(application));
		JplootConfigFile newConfig = new ImmutableJplootConfigFile.Builder()
				.from(currentConfig)
				.applications(newApplications)
				.build();
		try {
			String newConfigContent = mapper.writeValueAsString(newConfig);
			try (Writer writer = new FileWriter(config.location().toFile())) {
				writer.write(newConfigContent);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Error saving configuration to %s", config.location()), e);
			}
			return load(config.location());
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error serializing configuration", e);
		}
	}

	private JplootApplicationFile applicationFile(JplootApplication application) {
		return new ImmutableJplootApplicationFile.Builder()
				.name(application.name())
				.groupId(application.groupId())
				.artifactId(application.artifactId())
				.dependencies(application.dependencies().stream().map(this::dependencyFile).collect(Collectors.toSet()))
				.description(application.description())
				.mainClass(application.mainClass())
				.version(application.version())
				.build();
	}

	private JplootDependencyFile dependencyFile(JplootDependency dependency) {
		return new ImmutableJplootDependencyFile.Builder()
				.groupId(dependency.groupId())
				.artifactId(dependency.artifactId())
				.version(dependency.version())
				.allowedSources(dependency.allowedSources())
				.types(dependency.types())
				.build();
	}

	private Set<JplootApplication> applications(JplootConfigFile from) throws MissingConfigException {
		Set<JplootApplication> applications = new HashSet<>();
		for (JplootApplicationFile i : from.applications().orElse(new HashSet<>())) {
			applications.add(application(i));
		}
		return applications;
	}

	private Set<JavaRuntime> runtimes(JplootConfigFile from) throws MissingConfigException {
		Set<JavaRuntime> runtimes = new HashSet<>();
		for (JavaRuntimeFile i : from.runtimes().orElse(new HashSet<>())) {
			runtimes.add(runtime(i));
		}
		if (runtimes.isEmpty()) {
			runtimes.add(ImmutableJavaRuntime.builder()
					.name("default")
					.version("11")
					.javaHome(Path.of("/usr/lib/jvm/java-11"))
					.build());
		}
		return runtimes;
	}

	private Set<MavenRepository> mavenRepositories(JplootConfigFile from) throws MissingConfigException {
		Set<MavenRepository> repositories = new HashSet<>();
		for (MavenRepositoryFile i : from.mavenRepositories().orElse(new HashSet<>())) {
			repositories.add(mavenRepository(i));
		}
		if (repositories.isEmpty()) {
			repositories.add(ImmutableMavenRepository.builder()
					.name("default")
					.location(Path.of(System.getProperty("user.home"), ".m2/repository"))
					.build());
		}
		return repositories;
	}

	private JplootApplication application(JplootApplicationFile from) throws MissingConfigException {
		ImmutableJplootApplication.Builder builder = ImmutableJplootApplication.builder();
		builder.dependencies(dependencies(from.dependencies()));
		builder.addTypes(DependencyType.CLASSPATH);
		builder.addAllowedSources(DependencySource.JPLOOT);
		builder.name(get("name", from.name()));
		builder.groupId(get("groupId", from.groupId()));
		builder.artifactId(get("artifactId", from.artifactId()));
		builder.version(get("version", from.version())); //NOSONAR
		builder.description(from.description());
		builder.mainClass(from.mainClass());
		return builder.build();
	}

	private JavaRuntime runtime(JavaRuntimeFile from) throws MissingConfigException {
		ImmutableJavaRuntime.Builder builder = ImmutableJavaRuntime.builder();
		builder.name(get("name", from.name()));
		builder.javaHome(get("javaHome", from.javaHome()));
		builder.version(get("version", from.version()));
		return builder.build();
	}

	private MavenRepository mavenRepository(MavenRepositoryFile from) throws MissingConfigException {
		ImmutableMavenRepository.Builder builder = ImmutableMavenRepository.builder();
		builder.name(get("name", from.name()));
		builder.location(get("location", from.location()));
		return builder.build();
	}

	private Set<JplootDependency> dependencies(Optional<Set<JplootDependencyFile>> from) throws MissingConfigException {
		Set<JplootDependency> result = new HashSet<>();
		for (JplootDependencyFile i : from.orElse(new HashSet<>())) {
			result.add(dependency(i));
		}
		return result;
	}

	private JplootDependency dependency(JplootDependencyFile from) throws MissingConfigException {
		ImmutableJplootDependency.Builder builder = ImmutableJplootDependency.builder();
		builder.addTypes(DependencyType.CLASSPATH);
		builder.groupId(get("groupId", from.groupId()));
		builder.artifactId(get("artifactId", from.artifactId()));
		builder.version(get("version", from.version()));
		builder.allowedSources(from.allowedSources().orElse(new HashSet<>()));
		builder.types(from.types().orElse(new HashSet<>()));
		return builder.build();
	}

	public MissingConfigException throwOnMissingConfig(String name) {
		return new MissingConfigException(name);
	}

	<T> T get(String name, Optional<T> optional) throws MissingConfigException {
		try {
			return optional.get(); //NOSONAR
		} catch (NoSuchElementException e) {
			throw new MissingConfigException(name);
		}
	}

	<T> T get(String name, T value) throws MissingConfigException {
		if (value != null) {
			return value;
		} else {
			throw new MissingConfigException(name);
		}
	}

	private class MissingConfigException extends Exception {
		private static final long serialVersionUID = -8162741671486748445L;
		
		final String name;
		public MissingConfigException(String name) {
			this.name = name;
		}
		public String getConfigName() {
			return name;
		}
	}
}
