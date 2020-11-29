package jploot.config.loader;

import java.nio.file.Path;

import jploot.config.model.ImmutableJavaRuntime;
import jploot.config.model.ImmutableJplootBase;
import jploot.config.model.ImmutableJplootConfig;
import jploot.config.model.JplootConfig;

public class JplootConfigLoader {

	public JplootConfigLoader() {
		super();
	}

	public JplootConfig load() {
		return ImmutableJplootConfig.builder()
				.jplootBase(ImmutableJplootBase.builder()
						.location(Path.of("jploot"))
						.addJavaRuntimes(ImmutableJavaRuntime.builder()
								.name("java-11")
								.javaHome(Path.of("/usr/lib/jvm/java-11"))
								.version("11")
								.build())
						.build())
				.build();
	}

}
