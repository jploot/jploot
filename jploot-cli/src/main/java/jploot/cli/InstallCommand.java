package jploot.cli;

import java.nio.file.Path;
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
import jploot.core.installer.JplootInstaller;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "install",
	mixinStandardHelpOptions = false,
	description = "Install a jploot application")
public class InstallCommand implements Callable<Integer> {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstallCommand.class);

	@Spec
	private CommandSpec spec;

	@picocli.CommandLine.Option(names = { "-g", "--groupId" }, required = true)
	private String groupId;

	@picocli.CommandLine.Option(names = { "-a", "--artifactId" }, required = true)
	private String artifactId;

	@picocli.CommandLine.Option(names = { "-v", "--version" }, required = true)
	private String version;

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
		if (!config.repository().toFile().isDirectory()) {
			config.repository().toFile().mkdirs();
		}
		new JplootInstaller().run(config, application);
		int status = 0;
		LOGGER.info("jploot ending with status {}", status);
		return status;
	}

}