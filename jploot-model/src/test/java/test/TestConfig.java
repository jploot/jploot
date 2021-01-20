package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jploot.config.model.ImmutableJplootConfig;

public class TestConfig {

	/**
	 * jplootBase must be resolved either is config location is a relative filename
	 */
	@Test
	public void test_relativeConfig_jplootBase() {
		assertThat(
				ImmutableJplootConfig.builder()
					.jplootHome(Path.of("."))
					.location(Path.of("file.yml")).build().jplootBase())
		.isDirectory();
	}

}
