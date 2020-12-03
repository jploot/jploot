package test;

import org.junit.jupiter.api.Test;

import jploot.config.model.ImmutableJplootConfig;
import jploot.core.runner.JplootRunner;

public class TestRunner extends AbstractTest {

	@Test
	void testRunner() {
		JplootRunner runner = new JplootRunner();
		ImmutableJplootConfig config = jplootConfigBuilder.build();
		runner.run(config, applicationBuilder.build());
	}

}
