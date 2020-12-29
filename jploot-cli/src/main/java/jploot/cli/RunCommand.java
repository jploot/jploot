package jploot.cli;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.JplootApplication;
import jploot.core.runner.JplootRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "run",
	mixinStandardHelpOptions = true,
	description = "Run a jploot application")
public class RunCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunCommand.class);

	@Parameters(index = "0")
	private String applicationName;

	@Parameters(index = "1..*")
	List<String> params;

	@Override
	public Integer doCall() {
		LOGGER.trace("🐛 Run starting");
		Optional<JplootApplication> candidate = findApplication(applicationName);
		if (candidate.isEmpty()) {
			LOGGER.error("Application {} cannot be found", applicationName);
			return 1;
		} else {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("🔍 Application found: {}", candidate.get().asSpec());
			}
			new JplootRunner().run(config(), candidate.get(), params != null ? params : Collections.emptyList());
			LOGGER.info("📌 Run done");
			return 0;
		}
	}

}
