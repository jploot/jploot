package test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jploot.config.model.ImmutableJplootDependency;

public class TestEquality {

	@Test
	public void test_JplootDependency_equals() {
		assertThat(ImmutableJplootDependency.builder().groupId("g").artifactId("a").version("1.0").build())
			.isEqualTo(ImmutableJplootDependency.builder().groupId("g").artifactId("a").version("1.0").build());
	}

	@Test
	public void test_JplootDependency_group_notEquals() {
		assertThat(ImmutableJplootDependency.builder().groupId("g1").artifactId("a").version("1.0").build())
			.isNotEqualTo(ImmutableJplootDependency.builder().groupId("g2").artifactId("a").version("1.0").build());
	}

	@Test
	public void test_JplootDependency_artifact_notEquals() {
		assertThat(ImmutableJplootDependency.builder().groupId("g").artifactId("a1").version("1.0").build())
			.isNotEqualTo(ImmutableJplootDependency.builder().groupId("g").artifactId("a2").version("1.0").build());
	}

	@Test
	public void test_JplootDependency_version_notEquals() {
		assertThat(ImmutableJplootDependency.builder().groupId("g").artifactId("a").version("1.1").build())
			.isNotEqualTo(ImmutableJplootDependency.builder().groupId("g").artifactId("a").version("1.2").build());
	}

}
