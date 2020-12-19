package jploot.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.loader.FileLoader;
import jploot.config.loader.JplootConfigLoader;
import jploot.config.model.ArgumentConfig;
import jploot.config.model.DependencySource;
import jploot.config.model.DependencyType;
import jploot.config.model.ImmutableArgumentConfig;
import jploot.config.model.ImmutableJplootApplication;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.core.runner.JplootRunner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
		name = "jploot",
		mixinStandardHelpOptions = true,
		description = "Java package management tool"
)
public class Main implements Callable<Integer> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	@Parameters
	List<String> params;

	public static final void main(String[] args) {
		Main command = new Main();
		int status = new CommandLine(command).execute(args);
		System.exit(status);
	}

	@Override
	public Integer call() throws Exception {
		LOGGER.info("jploot starting");
		ArgumentConfig args = ImmutableArgumentConfig.builder()
			.location(Path.of(System.getProperty("user.home"), ".config/jploot/config.yml"))
			.build();
		JplootApplication application = ImmutableJplootApplication.builder()
				.name("jploot")
				.groupId("jploot")
				.artifactId("jploot-cli")
				.version("1.0-SNAPSHOT")
				.mainClass("jploot.cli.Test")
				.addTypes(DependencyType.CLASSPATH)
				.addAllowedSources(DependencySource.MAVEN)
				.build();
		JplootConfig config = new JplootConfigLoader(new FileLoader()).load(args);
		new JplootRunner().run(config, application, params);
		int status = 0;
		LOGGER.info("jploot ending with status {}", status);
		return status;
	}

}
