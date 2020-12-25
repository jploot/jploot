package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import java.nio.file.Path;

import org.assertj.core.api.IterableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jploot.api.IJplootConfigLoader;
import jploot.config.loader.FileLoader;
import jploot.config.loader.JplootConfigLoader;
import jploot.config.model.JavaRuntime;
import jploot.config.model.JplootConfig;
import jploot.config.model.MavenRepository;

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
		IJplootConfigLoader configManager = new JplootConfigLoader(fileLoader);
		JplootConfig config = configManager.load(configPath);
		
		assertThat(config.location()).isEqualTo(configPath);
		
		IterableAssert<JavaRuntime> runtimesAssertions = assertThat(config.runtimes()).as("%s", config.runtimes());
		runtimesAssertions.hasSize(1);
		
		runtimesAssertions.first().satisfies(r -> {
			assertThat(r.name()).isEqualTo("default");
			assertThat(r.javaHome()).isEqualTo(Path.of("/usr/lib/jvm/java-11"));
			assertThat(r.version()).isEqualTo("11");
		});
		
		IterableAssert<MavenRepository> mavenRepositoriesAssertions = assertThat(config.mavenRepositories()).as("%s", config.mavenRepositories());
		mavenRepositoriesAssertions.hasSize(1);
		
		mavenRepositoriesAssertions.first().satisfies(r -> {
			assertThat(r.name()).isEqualTo("default");
			assertThat(r.location()).isEqualTo(Path.of(System.getProperty("user.home"), ".m2/repository"));
		});
	}

	@Test
	void testLoadConfig() {
		initMocks("---\n"
				+ "runtimes:\n"
				+ "  - name: test\n"
				+ "    javaHome: /path/\n"
				+ "    version: 11\n");
		IJplootConfigLoader configManager = new JplootConfigLoader(fileLoader);
		JplootConfig config = configManager.load(configPath);
		
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
