package jploot.cli;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.loader.FileLoader;
import jploot.config.loader.JplootConfigLoader;
import jploot.config.model.JplootConfig;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

@Command(
	name = "jploot",
	mixinStandardHelpOptions = true,
	description = "Java package management tool",
	subcommands = {
			RunCommand.class,
			InstallerCommand.class,
			InstallCommand.class,
			ListCommand.class,
			RemoveCommand.class,
			RepositoryCommand.class
	}
)
public class JplootMain implements IExecutionExceptionHandler {

	private static final String LOG4J2_PROPERTY_PREFIX = "log4j2.prefix";
	private static final String LOG4J2_PROPERTY_CONFIG_THROWABLE = "log4j2.config.throwable";
	private static final String LOG4J2_PROPERTY_CLI_LEVEL = "log4j2.cliLevel";
	private static final String LOG4J2_PROPERTY_MODULE_LEVEL = "log4j2.moduleLevel";
	private static final String LOG4J2_PROPERTY_THIRD_PARTY_LEVEL = "log4j2.thirdPartyLevel";

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootMain.class);

	@ArgGroup(exclusive = true)
	Verbosity verbosity;

	@Option(
			names = { "-c", "--config" },
			description = {
					"Jploot configuration file (yaml format)"
			},
			defaultValue = "${user.home}/.config/jploot/config.yml"
	)
	Path config;

	JplootConfig jplootConfig;

	PrintStream out;

	PrintStream err;

	public static final void main(String[] args) {
		LOGGER.debug("Command processing");
		JplootMain command = new JplootMain();
		command.out = new PrintStream(new BufferedOutputStream(System.out), true);
		command.err = new PrintStream(new BufferedOutputStream(System.err), true);
		CommandLine commandLine = new CommandLine(command);
		commandLine.setExecutionExceptionHandler(command);
		int status = commandLine.execute(args);
		if (status == 0) {
			LOGGER.trace("Command terminated successfully");
		} else if (status == 2) {
			// help message
		} else {
			LOGGER.warn("Command terminated with error");
		}
		System.exit(status);
	}

	void init(boolean needsConfig) {
		initLogging();
		if (needsConfig) {
			initConfiguration();
		}
	}

	private void initConfiguration() {
		LOGGER.trace("⏳ Configuration loading");
		LOGGER.info("📄 Configuration file: {}", config);
		jplootConfig = new JplootConfigLoader(new FileLoader()).load(config);
		LOGGER.debug("👌 Configuration loading");
	}

	private void initLogging() {
		LOGGER.trace("⏳ Logging verbosity configuration");
		reconfigure();
		LOGGER.debug("📝 Logging verbosity configuration");
	}

	public void reconfigure() {
		int verbosityLevel = verbosityLevel();
		boolean quiet = verbosity != null && verbosity.quiet;
		// TODO: handle throwable
		if (verbosityLevel == 3) {
			System.setProperty(LOG4J2_PROPERTY_CONFIG_THROWABLE, "%throwable");
		} else {
			System.setProperty(LOG4J2_PROPERTY_CONFIG_THROWABLE, "%notEmpty{ -%throwable{short.message}{separator()}}");
		}
		if (quiet) {
			System.setProperty(LOG4J2_PROPERTY_CLI_LEVEL, "error");
		} else if (verbosityLevel == 0) {
			System.setProperty(LOG4J2_PROPERTY_CLI_LEVEL, "info");
		} else if (verbosityLevel == 1) {
			System.setProperty(LOG4J2_PROPERTY_CLI_LEVEL, "debug");
		} else if (verbosityLevel == 2) {
			System.setProperty(LOG4J2_PROPERTY_CLI_LEVEL, "debug");
			System.setProperty(LOG4J2_PROPERTY_MODULE_LEVEL, "info");
		} else if (verbosityLevel == 3) {
			System.setProperty(LOG4J2_PROPERTY_CLI_LEVEL, "debug");
			System.setProperty(LOG4J2_PROPERTY_MODULE_LEVEL, "debug");
			System.setProperty(LOG4J2_PROPERTY_THIRD_PARTY_LEVEL, "debug");
		} else {
			System.setProperty(LOG4J2_PROPERTY_CLI_LEVEL, "trace");
			System.setProperty(LOG4J2_PROPERTY_MODULE_LEVEL, "trace");
			System.setProperty(LOG4J2_PROPERTY_THIRD_PARTY_LEVEL, "debug");
		}
		
		System.setProperty(LOG4J2_PROPERTY_PREFIX, "%level{WARN=❗, DEBUG=0, ERROR=❌, TRACE=0, INFO=0}");
		((LoggerContext) LogManager.getContext(false)).reconfigure();
	}

	public int verbosityLevel() {
		return (verbosity != null && verbosity.verbosityLevel != null) ? verbosity.verbosityLevel.length : 0;
	}

	private static class Verbosity {
		@Option(
				names = "-v",
				description = {
						"Specify multiple -v options to increase verbosity.",
						"For example, `-v -v -v` or `-vvv`"
				}
		)
		boolean[] verbosityLevel = new boolean[0];
		
		@Option(
				names = { "-q", "--quiet" },
				description = {
						"Quiet output - only show errors"
				}
		)
		private boolean quiet;
	}

	@Override
	public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult)
			throws Exception {
		LOGGER.error(ex.getMessage(), ex);
		return 128;
	}

}
