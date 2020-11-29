package jploot.cli;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
		name = "jploot",
		mixinStandardHelpOptions = true,
		description = "Java package management tool"
)
public class Main implements Callable<Integer> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static final void main(String[] args) {
		Main command = new Main();
		int status = new CommandLine(command).execute(args);
		System.exit(status);
	}

	@Override
	public Integer call() throws Exception {
		LOGGER.info("jploot starting");
		int status = 0;
		LOGGER.info("jploot ending with status {}", status);
		return status;
	}

}
