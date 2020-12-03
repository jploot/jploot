package test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jploot.config.loader.FileLoader;
import jploot.config.loader.JplootConfigLoader;
import jploot.config.model.ImmutableArgumentConfig;
import jploot.config.model.JplootConfig;

class TestLoadConfig {

	@Test
	void testLoadConfig() {
		FileLoader fileLoader = mock(FileLoader.class);
		Mockito.when(fileLoader.load(any())).thenReturn("---\n"
				+ "runtimes:\n"
				+ "  - name: test\n"
				+ "    javaHome: /path/\n"
				+ "    version: 11\n");
		JplootConfigLoader loader = new JplootConfigLoader(fileLoader);
		JplootConfig config = loader.load(ImmutableArgumentConfig.builder().location(Path.of("any")).build());
	}

}
