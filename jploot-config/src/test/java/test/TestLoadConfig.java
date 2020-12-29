package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.assertj.core.api.IterableAssert;
import org.assertj.core.api.ListAssert;
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

@ExtendWith(MockitoExtension.class)
class TestLoadConfig {

	@Mock
	private FileLoader fileLoader;

	private Path configPath = Path.of("marker");

	void initMocks(String yaml) {
		Mockito.when(fileLoader.load(eq(configPath), eq(FileLoader.Mode.YAML))).thenReturn(yaml);
	}

	@Test
	void testDefaultConfig() throws URISyntaxException {
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
		
		ListAssert<URI> mavenRepositoriesAssertions = assertThat(config.repositories()).as("%s", config.repositories());
		mavenRepositoriesAssertions.containsExactly(
				new URI("https://repo.maven.apache.org/maven2"),
				new URI("https://dl.bintray.com/jploot/jploot"));
	}

	@Test
	void testLoadConfig() throws URISyntaxException {
		initMocks("---\n"
				+ "runtimes:\n"
				+ "  - name: test\n"
				+ "    javaHome: /path/\n"
				+ "    version: 11\n"
				+ "repositories:\n"
				+ "  - http://localhost:8081/repository/jploot-releases/\n");
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
		
		ListAssert<URI> mavenRepositoriesAssertions = assertThat(config.repositories()).as("%s", config.repositories());
		mavenRepositoriesAssertions.containsExactly(
				new URI("https://repo.maven.apache.org/maven2"),
				new URI("https://dl.bintray.com/jploot/jploot"),
				new URI("http://localhost:8081/repository/jploot-releases/"));
	}

}
