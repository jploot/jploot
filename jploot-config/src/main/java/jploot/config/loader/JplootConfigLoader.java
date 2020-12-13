package jploot.config.loader;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import jploot.config.model.ArgumentConfig;
import jploot.config.model.DependencyType;
import jploot.config.model.ImmutableJavaRuntime;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootConfig;
import jploot.config.model.ImmutableJplootDependency;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.config.model.JplootDependency;
import jploot.config.model.yaml.JavaRuntimeFile;
import jploot.config.model.yaml.JplootApplicationFile;
import jploot.config.model.yaml.JplootConfigFile;
import jploot.config.model.yaml.JplootDependencyFile;

/**
 * <p>
 * Load a jploot config file from a {@link ArgumentConfig}. Default configuration are applied in case configuration
 * file is incomplete.
 * </p>
 */
public class JplootConfigLoader {

	private final ObjectMapper mapper;
	private final FileLoader fileLoader;

	public JplootConfigLoader(FileLoader fileLoader) {
		super();
		this.fileLoader = fileLoader;
		mapper = new ObjectMapper(new YAMLFactory());
		// handle Optional types
		mapper.registerModule(new Jdk8Module());
	}

	public JplootConfig load(ArgumentConfig config) {
		String content = fileLoader.load(config.location(), FileLoader.Mode.YAML);
		try {
			JplootConfigFile configFile = mapper.readValue(content, JplootConfigFile.class);
			ImmutableJplootConfig.Builder builder = ImmutableJplootConfig.builder()
					.location(config.location());
			builder.applications(applications(configFile));
			builder.runtimes(runtimes(configFile));
			return builder.build();
		} catch (JsonProcessingException|MissingConfigException e) {
			throw new IllegalStateException(e);
		}
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

	private JplootApplication application(JplootApplicationFile from) throws MissingConfigException {
		ImmutableJplootApplication.Builder builder = ImmutableJplootApplication.builder();
		builder.dependencies(dependencies(from.dependencies()));
		builder.addTypes(DependencyType.CLASSPATH);
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
		final String name;
		public MissingConfigException(String name) {
			this.name = name;
		}
		public String getConfigName() {
			return name;
		}
	}
}
