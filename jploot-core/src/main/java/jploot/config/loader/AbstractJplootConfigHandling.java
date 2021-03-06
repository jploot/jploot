package jploot.config.loader;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import jploot.api.IFileLoader;
import jploot.config.model.ImmutableJavaRuntime;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootConfig;
import jploot.config.model.ImmutableJplootDependency;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.config.model.JplootDependency;
import jploot.config.model.yaml.ImmutableJplootApplicationFile;
import jploot.config.model.yaml.ImmutableJplootDependencyFile;
import jploot.config.model.yaml.JavaRuntimeFile;
import jploot.config.model.yaml.JplootApplicationFile;
import jploot.config.model.yaml.JplootConfigFile;
import jploot.config.model.yaml.JplootDependencyFile;
import jploot.exceptions.ConfigException;
import jploot.exceptions.ConfigMissingValueException;

public class AbstractJplootConfigHandling {

	private static final URI MAVEN_CENTRAL_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");
	private static final URI JPLOOT_REPOSITORY = URI.create("https://nexus.tools.kobalt.fr/repository/jploot/");

	protected IFileLoader fileLoader;
	protected ObjectMapper mapper;

	public AbstractJplootConfigHandling(IFileLoader fileLoader) {
		super();
		this.fileLoader = fileLoader;
		mapper = new ObjectMapper(new YAMLFactory());
		// handle Optional type
		mapper.registerModule(new Jdk8Module());
	}

	protected JplootConfig load(Path location) {
		try {
			JplootConfigFile configFile = loadJplootConfigFile(location);
			ImmutableJplootConfig.Builder builder = ImmutableJplootConfig.builder()
					.location(location);
			builder.applications(applications(configFile));
			builder.runtimes(runtimes(configFile));
			builder.repositories(repositories(configFile));
			String jplootHomeEnv = System.getenv("JPLOOT_HOME");
			String jplootHomeProperty = System.getProperty("jploot.home", null);
			if (jplootHomeEnv != null) {
				builder.jplootHome(Path.of(jplootHomeEnv));
			} else if (jplootHomeProperty != null) {
				builder.jplootHome(Path.of(jplootHomeProperty));
			} else {
				throw new ConfigMissingValueException("Both JPLOOT_HOME and jploot.home");
			}
			return builder.build();
		} catch (ConfigMissingValueException e) {
			throw new IllegalStateException(e);
		}
	}

	protected JplootConfigFile loadJplootConfigFile(Path location) {
		String content = fileLoader.load(location, IFileLoader.Mode.YAML);
		try {
			return mapper.readValue(content, JplootConfigFile.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	protected JplootApplicationFile applicationFile(JplootApplication application) {
		return new ImmutableJplootApplicationFile.Builder()
				.name(application.name())
				.groupId(application.groupId())
				.artifactId(application.artifactId())
				.dependencies(application.dependencies().stream().map(this::dependencyFile).collect(Collectors.toSet()))
				.description(application.description())
				.mainClass(application.mainClass())
				.version(application.version())
				.launchers(application.launchers())
				.build();
	}

	private JplootDependencyFile dependencyFile(JplootDependency dependency) {
		return new ImmutableJplootDependencyFile.Builder()
				.groupId(dependency.groupId())
				.artifactId(dependency.artifactId())
				.version(dependency.version())
				.build();
	}

	private Set<JplootApplication> applications(JplootConfigFile from) throws ConfigMissingValueException {
		Set<JplootApplication> applications = new HashSet<>();
		for (JplootApplicationFile i : from.applications().orElse(new HashSet<>())) {
			applications.add(application(i));
		}
		return applications;
	}

	private Set<JavaRuntime> runtimes(JplootConfigFile from) throws ConfigMissingValueException {
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

	private List<URI> repositories(JplootConfigFile from) throws ConfigMissingValueException {
		List<URI> repositories = new ArrayList<>();
		for (String i : from.repositories().orElse(new ArrayList<>())) {
			repositories.add(repository(i));
		}
		if (!repositories.contains(JPLOOT_REPOSITORY)) {
			repositories.add(0, JPLOOT_REPOSITORY);
		}
		if (!repositories.contains(MAVEN_CENTRAL_REPOSITORY)) {
			repositories.add(0, MAVEN_CENTRAL_REPOSITORY);
		}
		return repositories;
	}

	private JplootApplication application(JplootApplicationFile from) throws ConfigMissingValueException {
		ImmutableJplootApplication.Builder builder = ImmutableJplootApplication.builder();
		builder.dependencies(dependencies(from.dependencies()));
		builder.name(get("name", from.name()));
		builder.groupId(get("groupId", from.groupId()));
		builder.artifactId(get("artifactId", from.artifactId()));
		builder.version(get("version", from.version())); //NOSONAR
		builder.description(from.description());
		builder.mainClass(from.mainClass());
		builder.launchers(from.launchers());
		return builder.build();
	}

	private JavaRuntime runtime(JavaRuntimeFile from) throws ConfigMissingValueException {
		ImmutableJavaRuntime.Builder builder = ImmutableJavaRuntime.builder();
		builder.name(get("name", from.name()));
		builder.javaHome(get("javaHome", from.javaHome()));
		builder.version(get("version", from.version()));
		return builder.build();
	}

	private URI repository(String from) throws ConfigMissingValueException {
		try {
			return new URI(from);
		} catch (URISyntaxException e) {
			throw new ConfigException(String.format("Failed to parse repository URI %s", from), e);
		}
	}

	private Set<JplootDependency> dependencies(Optional<Set<JplootDependencyFile>> from) throws ConfigMissingValueException {
		Set<JplootDependency> result = new HashSet<>();
		for (JplootDependencyFile i : from.orElse(new HashSet<>())) {
			result.add(dependency(i));
		}
		return result;
	}

	private JplootDependency dependency(JplootDependencyFile from) throws ConfigMissingValueException {
		ImmutableJplootDependency.Builder builder = ImmutableJplootDependency.builder();
		builder.groupId(get("groupId", from.groupId()));
		builder.artifactId(get("artifactId", from.artifactId()));
		builder.version(get("version", from.version()));
		return builder.build();
	}

	public ConfigMissingValueException throwOnMissingConfig(String name) {
		return new ConfigMissingValueException(name);
	}

	<T> T get(String name, Optional<T> optional) throws ConfigMissingValueException {
		try {
			return optional.get(); //NOSONAR
		} catch (NoSuchElementException e) {
			throw new ConfigMissingValueException(name);
		}
	}

	<T> T get(String name, T value) throws ConfigMissingValueException {
		if (value != null) {
			return value;
		} else {
			throw new ConfigMissingValueException(name);
		}
	}

}