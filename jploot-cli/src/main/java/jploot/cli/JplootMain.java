package jploot.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
	name = "jploot",
	mixinStandardHelpOptions = true,
	description = "Java package management tool",
	subcommands = {
			RunCommand.class,
			BootstrapCommand.class,
			InstallCommand.class
	}
)
public class JplootMain {

	@Option(
			names = "-v",
			description = {
					"Specify multiple -v options to increase verbosity.",
					"For example, `-v -v -v` or `-vvv`"
			}
	)
	boolean[] verbosity;

	public static final void main(String[] args) {
		JplootMain command = new JplootMain();
		int status = new CommandLine(command).execute(args);
		System.exit(status);
	}

}
