package jploot.cli;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.JplootApplication;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "remove",
	mixinStandardHelpOptions = true,
	description = "Remove a jploot application")
public class RemoveCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemoveCommand.class);

	@Parameters(
			index = "0",
			arity = "1",
			paramLabel = "APPLICATION",
			description = { "groupId:artifactId:version or name:version",
					"Application to remove" }
	)
	private String applicationName;

	@Override
	public Integer doCall() {
		LOGGER.trace("🐛 Remove starting");
		Optional<JplootApplication> candidate = findApplication(applicationName);
		if (candidate.isEmpty()) {
			LOGGER.error("Application {} cannot be found", applicationName);
			return 1;
		} else {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("🔍 Application found: {}", candidate.get().asSpec());
			}
			configUpdater().removeApplication(candidate.get());
			launcherManager().removeLaunchers(candidate.get());
			LOGGER.info("📌 Remove done");
			return 0;
		}
	}

}
