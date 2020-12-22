package jploot.cli;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.loader.FileLoader;
import jploot.config.loader.JplootConfigManager;
import jploot.config.model.ArgumentConfig;
import jploot.config.model.DependencySource;
import jploot.config.model.DependencyType;
import jploot.config.model.ImmutableArgumentConfig;
import jploot.config.model.ImmutableJplootDependency;
import jploot.config.model.JplootConfig;
import jploot.config.model.JplootDependency;
import jploot.core.installer.JplootInstaller;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "install",
	mixinStandardHelpOptions = false,
	description = "Install a jploot application")
public class InstallCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstallCommand.class);

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
		JplootDependency application = ImmutableJplootDependency.builder()
				.groupId(groupId)
				.artifactId(artifactId)
				.version("1.0-SNAPSHOT")
				.addTypes(DependencyType.CLASSPATH)
				.addAllowedSources(DependencySource.MAVEN)
				.build();
		JplootConfigManager configManager = new JplootConfigManager(new FileLoader());
		JplootConfig config = configManager.load(args);
		if (!config.repository().toFile().isDirectory()) {
			config.repository().toFile().mkdirs();
		}
		new JplootInstaller().install(config, configManager, application);
		int status = 0;
		LOGGER.info("jploot ending with status {}", status);
		return status;
	}

}