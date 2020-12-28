package jploot.cli;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.config.model.ImmutableJplootDependency;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootDependency;
import jploot.core.installer.JplootInstaller;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "install",
	mixinStandardHelpOptions = false,
	description = "Install a jploot application")
public class InstallCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstallCommand.class);

	/**
	 * Artifact definition (group, artifact, version) TODO 0.1 use a spec
	 */
	@ArgGroup(exclusive = false, multiplicity="1", heading = "Artifact identifier")
	Artifact artifact;

	@Override
	public Integer doCall() {
		LOGGER.trace("🐛 Install starting");
		LOGGER.trace("🐛 Application descriptor building");
		JplootDependency application = ImmutableJplootDependency.builder()
				.groupId(artifact.groupId)
				.artifactId(artifact.artifactId)
				.version(artifact.version)
				.build();
		Optional<JplootApplication> candidate = findApplication(application);
		if (candidate.isPresent()) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Application {} is already installed ({})",
						candidate.get().asSpec(), candidate.get().name());
			}
			return 0;
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("⏳ Install {}", application.asSpec());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.trace("🐛 Application descriptor: {}", application.toDebug());
		}
		if (!config().repository().toFile().isDirectory()) {
			config().repository().toFile().mkdirs();
		}
		new JplootInstaller().install(config().repositories(), configUpdater(), repositoryUpdater(), application);
		LOGGER.info("📌 Install done");
		return 0;
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