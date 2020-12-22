package jploot.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;

@Command(name = "bootstrap",
	mixinStandardHelpOptions = true,
	description = "Bootstrap jploot")
public class BootstrapCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapCommand.class);

	@Override
	public Integer call() throws Exception {
		String javaHome = System.getProperty("java.home");
		Path javaHomePath = Path.of(javaHome);
		if (!javaHomePath.toFile().isDirectory()) {
			LOGGER.error("Missing JVM environment: {}", javaHomePath);
			return 1;
		}
		// TODO: use XDG_* if available
		// TODO: allow in-folder installation
		Path userHome = Path.of(System.getProperty("user.home"));
		Path target = userHome.resolve(".local/share").resolve("jploot");
		Path targetConf = userHome.resolve(".config").resolve("jploot");
		Path jplootJre = target.resolve("jploot-jre");
		if (jplootJre.toFile().exists()) {
			LOGGER.warn("Jploot JRE already exists, skipping install: {}", javaHomePath);
		} else {
			createIfNotExists(jplootJre);
			copyDirectory(javaHomePath, jplootJre);
		}
		createIfNotExists(targetConf);
		
		LOGGER.info("Jploot installed in {}", target);
		
		return 0;
	}

	private void copyDirectory(Path javaHomePath, Path target) throws IOException {
		try (Stream<Path> walkStream = Files.walk(javaHomePath)) {
			walkStream.forEachOrdered(p -> {
				Path t = target.resolve(javaHomePath.relativize(p));
				LOGGER.debug("Copy: handling {} to {}", p, t);
				try {
					if (".".equals(p.getFileName().toString())
							|| "..".equals(p.getFileName().toString())) {
						return;
					}
					if (t.toFile().exists() && p.toFile().isDirectory()) {
						// already existing dirs cannot be removed
						return;
					}
					Files.copy(p, t, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new IllegalStateException(
							String.format("Error copying %s to %s", p, t), e);
				}
			});
		}
	}

	private void createIfNotExists(Path target) {
		File file = target.toFile();
		if (!file.isDirectory()) {
			file.mkdirs();
		}
	}

}