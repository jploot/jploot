package jploot.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.JplootApplication;
import picocli.CommandLine.Command;

@Command(name = "list",
	mixinStandardHelpOptions = false,
	description = "List installed jploot applications")
public class ListCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);

	@Override
	public Integer doCall() {
		LOGGER.trace("🐛 List starting");
		config().applications().forEach(this::printApplication);
		LOGGER.info("📌 List done");
		return 0;
	}

	void printApplication(JplootApplication application) {
		out().println(application.asSpec());
	}

	static class Artifact {
		@picocli.CommandLine.Option(names = { "-g", "--groupId" }, required = true)
		private String groupId;
	
		@picocli.CommandLine.Option(names = { "-a", "--artifactId" }, required = true)
		private String artifactId;
	
		@picocli.CommandLine.Option(names = { "-v", "--version" }, required = true)
		private String version;
	}

}