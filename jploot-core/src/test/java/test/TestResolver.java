package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jploot.config.exceptions.JplootArtifactFailure;
import jploot.config.model.ArtifactLookups;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootConfig;
import jploot.config.model.JplootBase;
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
		ArtifactLookups lookups = resolver.resolve(config, config.jplootBase(), application);
		
		// check call and context args
		verify(pathHandler).isValidArtifact(any(), eq(application), eq(config), eq(base));
		
		// check artifact path
		assertThat(lookups.failedLookups()).isEmpty();
		assertThat(lookups.lookups()).hasEntrySatisfying(application, lookup -> {
			assertThat(lookup.path())
				.describedAs("%s path attribute", lookup)
				.isPresent();
			
			Path path = lookup.path().get();
			
			assertThat(path)
				.describedAs("%s::path -> %s", lookup, path)
				.hasFileName(artifactFilename);
			assertThat(path)
				.describedAs("%s::path -> %s", lookup, path)
				.hasParentRaw(jplootBaseLocation);
		});
	}

	@Test
	void testResolveNotFound() throws JplootArtifactFailure {
		JplootArtifactFailure exception = mock(JplootArtifactFailure.class);
		doThrow(exception).when(pathHandler)
			.isValidArtifact(any(), any(), any(), any());
		ArtifactLookups lookups = resolver.resolve(config, base, application);
		assertThat(lookups.failedLookups())
			.describedAs("resolve(%s, %s, %s) -> %s", config, base, application, lookups)
			.satisfies(l -> {
				assertThat(l).size().isEqualTo(1);
				assertThat(l).first().satisfies(e -> {
					assertThat(e.getValue().failure()).contains(exception);
				});
			});
		
		// check call and context args
		verify(pathHandler).isValidArtifact(any(), eq(application), eq(config), eq(base));
	}

}
