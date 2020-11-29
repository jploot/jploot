package test;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCase.class);

	@Test
	public void testLoggingMessage() {
		LOGGER.warn("logging message");
	}

}
