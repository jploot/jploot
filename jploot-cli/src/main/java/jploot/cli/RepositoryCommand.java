package jploot.cli;

import picocli.CommandLine.Command;

@Command(
		name = "repository",
		mixinStandardHelpOptions = true,
		description = "Configure repositories.",
		subcommands = {
				RepositoryAddCommand.class,
				RepositoryRemoveCommand.class,
				RepositoryListCommand.class
				})
public class RepositoryCommand extends AbstractCommand {

	@Override
	protected Integer doCall() {
		return 0;
	}
}
