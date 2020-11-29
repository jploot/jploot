package test;

import org.junit.jupiter.api.Test;

import jploot.core.runner.JplootRunner;

public class TestJplootRunner extends AbstractTest {

	private JplootRunner runner = new JplootRunner();

	@Test
	public void testCommandNotFound() {
		runner.run(jplootBaseBuilder.build(), applicationBuilder.build());
	}

}
