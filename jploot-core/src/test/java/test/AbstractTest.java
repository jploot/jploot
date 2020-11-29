package test;

import java.nio.file.Path;

import jploot.config.model.ImmutableJavaRuntime;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootBase;
import jploot.config.model.ImmutableJplootConfig;

public class AbstractTest {
	protected ImmutableJplootBase.Builder jplootBaseBuilder = ImmutableJplootBase.builder()
			.name("default")
			.addJavaRuntimes(ImmutableJavaRuntime.builder()
					.name("default")
					.javaHome(Path.of("/usr/lib/jvm/java-11"))
					.version("11")
					.build())
			.location(Path.of(""));
	protected ImmutableJplootConfig.Builder jplootConfigBuilder = ImmutableJplootConfig.builder();
	protected ImmutableJplootApplication.Builder applicationBuilder = ImmutableJplootApplication.builder()
			.name("command")
			.groupId("command")
			.artifactId("command")
			.version("1.0");

}
