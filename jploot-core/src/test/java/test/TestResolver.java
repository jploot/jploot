package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootConfig;
import jploot.config.model.JplootBase;
import jploot.core.exceptions.JplootArtifactFailure;
import jploot.core.runner.spi.ArtifactResolver;
import jploot.core.runner.spi.PathHandler;

class TestResolver extends AbstractTest {

	private PathHandler pathHandler = mock(PathHandler.class);
	private ArtifactResolver resolver = new ArtifactResolver(pathHandler);
	private Path jplootBaseLocation = Path.of("my/location");
	private String groupId = "groupId";
	private String artifactId = "artifactId";
	private String version = "1.0";
	private String artifactFilename = String.format("%s-%s-%s.jar", groupId, artifactId, version);
	private JplootBase base = jplootBaseBuilder.location(jplootBaseLocation).build();
	private ImmutableJplootConfig config = jplootConfigBuilder.jplootBase(base).build();
	private ImmutableJplootApplication application = applicationBuilder.groupId(groupId)
			.artifactId(artifactId).version(version).build();

	@Test
	void testResolve() throws JplootArtifactFailure {
		Path artifactPath = resolver.resolve(config, config.jplootBase(), application);
		
		// check call and context args
		verify(pathHandler).isValidArtifact(any(), eq(application), eq(config), eq(base));
		
		// check artifact path
		assertThat(artifactPath.getFileName())
			.describedAs("%s filename", artifactPath)
			.hasToString(artifactFilename);
		assertThat(artifactPath)
			.describedAs("%s folder", artifactPath)
			.hasParentRaw(jplootBaseLocation);
	}

	@Test
	void testResolveNotFound() throws JplootArtifactFailure {
		JplootArtifactFailure exception = mock(JplootArtifactFailure.class);
		doThrow(exception).when(pathHandler)
			.isValidArtifact(any(), any(), any(), any());
		assertThatThrownBy(
				() -> resolver.resolve(config, base, application),
				"resolve(%s, %s, %s) thrown exception", config, base, application)
			.isSameAs(exception);
		
		// check call and context args
		verify(pathHandler).isValidArtifact(any(), eq(application), eq(config), eq(base));
	}

}
