package jploot.cli;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;

@Command(name = "list",
	mixinStandardHelpOptions = true,
	description = "List configured repositories.")
public class RepositoryListCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryListCommand.class);

	@Override
	public Integer doCall() {
		LOGGER.trace("🐛 List starting");
		config().repositories().forEach(this::printRepository);
		LOGGER.info("📌 List done");
		return 0;
	}

	void printRepository(URI repository) {
		out().println(repository.toString());
	}

}