package test;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jploot.config.model.ImmutableJavaRuntime;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootBase;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootBase;
import jploot.core.runner.JplootRunner;

public class TestJplootRunner {

	private JplootRunner runner = new JplootRunner();
	private JplootBase jplootBase = ImmutableJplootBase.builder()
			.name("default")
			.addJavaRuntimes(ImmutableJavaRuntime.builder()
					.name("default")
					.javaHome(Path.of("/usr/lib/jvm/java-11"))
					.version("11")
					.build())
			.location(Path.of(""))
			.build();
	private JplootApplication application = ImmutableJplootApplication.builder()
			.name("command")
			.groupId("command")
			.artifactId("command")
			.version("1.0")
			.build();

	@Test
	public void testCommandNotFound() {
		runner.run(jplootBase, application);
	}

}
