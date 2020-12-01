package test;

import org.junit.jupiter.api.Test;

import jploot.config.model.ImmutableJplootBase;
import jploot.config.model.ImmutableJplootConfig;
import jploot.core.runner.JplootRunner;

public class TestRunner extends AbstractTest {

	@Test
	void testRunner() {
		JplootRunner runner = new JplootRunner();
		ImmutableJplootBase base = jplootBaseBuilder.build();
		ImmutableJplootConfig config = jplootConfigBuilder.jplootBase(base).build();
		runner.run(config, base, applicationBuilder.build());
	}

}
