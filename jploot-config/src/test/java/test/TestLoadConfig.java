package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.PATH;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.eq;

import java.nio.file.Path;

import org.assertj.core.api.IterableAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jploot.config.loader.FileLoader;
import jploot.config.loader.JplootConfigLoader;
import jploot.config.model.ImmutableArgumentConfig;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootConfig;

@ExtendWith(MockitoExtension.class)
class TestLoadConfig {

	@Mock
	private FileLoader fileLoader;

	private Path configPath = Path.of("marker");

	void initMocks(String yaml) {
		Mockito.when(fileLoader.load(eq(configPath), eq(FileLoader.Mode.YAML))).thenReturn(yaml);
	}

	@Test
	void testDefaultConfig() {
		initMocks("{}");
		JplootConfigLoader loader = new JplootConfigLoader(fileLoader);
		JplootConfig config = loader.load(ImmutableArgumentConfig.builder().location(configPath).build());
		
		assertThat(config.location()).isEqualTo(configPath);
		
		IterableAssert<JavaRuntime> runtimesAssertions = assertThat(config.runtimes()).as("%s", config.runtimes());
		runtimesAssertions.hasSize(1);
		
		runtimesAssertions.first().satisfies(r -> {
			assertThat(r.name()).isEqualTo("default");
			assertThat(r.javaHome()).isEqualTo(Path.of("/usr/lib/jvm/java-11"));
			assertThat(r.version()).isEqualTo("11");
		});
		
	}

	@Test
	void testLoadConfig() {
		initMocks("---\n"
				+ "runtimes:\n"
				+ "  - name: test\n"
				+ "    javaHome: /path/\n"
				+ "    version: 11\n");
		JplootConfigLoader loader = new JplootConfigLoader(fileLoader);
		JplootConfig config = loader.load(ImmutableArgumentConfig.builder().location(configPath).build());
		
		assertThat(config.location()).isEqualTo(configPath);
		
		IterableAssert<JavaRuntime> runtimesAssertions = assertThat(config.runtimes()).as("%s", config.runtimes());
		runtimesAssertions.hasSize(1);
		
		runtimesAssertions.first().satisfies(r -> {
			assertThat(r.name()).isEqualTo("test");
			assertThat(r.javaHome()).isEqualTo(Path.of("/path"));
			assertThat(r.version()).isEqualTo("11");
		});
	}

}
