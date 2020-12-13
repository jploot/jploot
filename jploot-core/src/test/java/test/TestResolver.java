package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

import org.assertj.core.description.Description;
import org.assertj.core.description.TextDescription;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jploot.config.exceptions.JplootArtifactFailure;
import jploot.config.model.ArtifactLookups;
import jploot.config.model.DependencySource;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.ImmutableJplootConfig;
import jploot.config.model.ImmutableJplootDependency;
import jploot.core.runner.spi.ArtifactResolver;
import jploot.core.runner.spi.PathHandler;

class TestResolver extends AbstractTest {

	private PathHandler pathHandler = mock(PathHandler.class);
	private ArtifactResolver resolver = new ArtifactResolver(pathHandler);
	private Path jplootBaseLocation = Path.of("my/location");
	private Path jplootBaseArtifactsLocation = jplootBaseLocation.resolve("artifacts");
	private String groupId = "groupId";
	private String artifactId = "artifactId";
	private String version = "1.0";
	private String artifactFilename = String.format("%s-%s-%s.jar", groupId, artifactId, version);
	private ImmutableJplootConfig config = jplootConfigBuilder
			.location(jplootBaseLocation.resolve("config.yml"))
			.build();
	private ImmutableJplootApplication application = applicationBuilder.groupId(groupId)
			.artifactId(artifactId).version(version).build();

	@Test
	void testResolve() throws JplootArtifactFailure {
		ArtifactLookups lookups = resolver.resolve(config, application);
		
		// check call and context args
		verify(pathHandler).isValidArtifact(any(), eq(application), eq(application), eq(config));
		// check artifact path
		assertThat(lookups.failedLookups()).isEmpty();
		assertThat(lookups.lookups()).hasEntrySatisfying(application, lookup -> {
			Description desc = StackedDescription.describeAs("%s", lookup);
			assertThat(lookup.path())
				.as(StackedDescription.describeAs(desc, "path resolved path <%s>", lookup.path()))
				.isPresent();
			
			Path path = lookup.path().get();
			
			assertThat(path)
				.as(StackedDescription.describeAs(desc, "check resolved path <%s> filename", lookup.path()))
				.hasFileName(artifactFilename);
			assertThat(path)
				.as(StackedDescription.describeAs(desc, "check resolved path <%s> folder", lookup.path()))
				.hasParentRaw(jplootBaseArtifactsLocation);
			assertThat(lookup.source()).contains(DependencySource.JPLOOT);
		});
	}

	@Test
	void testResolveNotFound() throws JplootArtifactFailure {
		JplootArtifactFailure exception = mock(JplootArtifactFailure.class);
		doThrow(exception).when(pathHandler)
			.isValidArtifact(any(), any(), any(), any());
		ArtifactLookups lookups = resolver.resolve(config, application);
		assertThat(lookups.failedLookups())
			.describedAs("resolve(%s, %s, %s) -> %s", config, application, lookups)
			.satisfies(l -> {
				assertThat(l).size().isEqualTo(1);
				assertThat(l).first().satisfies(e -> {
					assertThat(e.getValue().failure()).contains(exception);
				});
			});
		
		// check call and context args
		verify(pathHandler).isValidArtifact(any(), eq(application), eq(application), eq(config));
	}

	@Test
	void testResolveJplootEmbedded() throws JplootArtifactFailure {
		ImmutableJplootDependency dependency = ImmutableJplootDependency.builder()
			.groupId("test")
			.artifactId("test")
			.version("1.0")
			.addAllowedSources(DependencySource.JPLOOT_EMBEDDED)
			.build();
		ImmutableJplootApplication application = applicationBuilder
				.groupId(groupId)
				.artifactId(artifactId)
				.version(version)
				.addDependencies(dependency).build();
		ArtifactLookups lookups = resolver.resolve(config, application);
		
		// check call and context args
		verify(pathHandler).isValidArtifact(any(), eq(application), eq(application), eq(config));
		Mockito.verifyNoMoreInteractions(pathHandler);
		// check artifact path
		assertThat(lookups.failedLookups()).isEmpty();
		assertThat(lookups.lookups()).hasEntrySatisfying(application, lookup -> {
			Description desc = StackedDescription.describeAs("%s", lookup);
			assertThat(lookup.path())
				.as(StackedDescription.describeAs(desc, "path resolved path <%s>", lookup.path()))
				.isPresent();
			
			Path path = lookup.path().get();
			
			assertThat(path)
				.as(StackedDescription.describeAs(desc, "check resolved path <%s> filename", lookup.path()))
				.hasFileName(artifactFilename);
			assertThat(path)
				.as(StackedDescription.describeAs(desc, "check resolved path <%s> folder", lookup.path()))
				.hasParentRaw(jplootBaseArtifactsLocation);
			assertThat(lookup.source()).contains(DependencySource.JPLOOT);
		});
	}

	// TODO: is it useful ?
	public static class StackedDescription extends Description {
		
		private final Description parent;
		private final Description description;
		
		private StackedDescription(Description parent, String text, Object... args) {
			super();
			this.parent = parent;
			this.description = new TextDescription(text, args);
		}
		
		@Override
		public String value() {
			return String.format("%s \n\tfrom %s", description.value(), parent.value());
		}
		
		public static TextDescription describeAs(String text, Object... args) {
			return new TextDescription(text, args);
		}
		
		public static StackedDescription describeAs(Description parent, String text, Object... args) {
			return new StackedDescription(parent, text, args);
		}
	}

}
