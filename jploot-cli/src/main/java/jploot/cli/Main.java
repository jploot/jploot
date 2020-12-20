package jploot.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
		name = "jploot",
		mixinStandardHelpOptions = true,
		description = "Java package management tool",
		subcommands = {
				RunCommand.class,
				BootstrapCommand.class
		}
)
public class Main {

	public static final void main(String[] args) {
		Main command = new Main();
		int status = new CommandLine(command).execute(args);
		System.exit(status);
	}

}
