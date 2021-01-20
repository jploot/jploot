package jploot.config.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.api.ILauncherManager;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.exceptions.InstallException;

public class JplootLauncherManager implements ILauncherManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(JplootLauncherManager.class);

	JplootConfig config;

	public JplootLauncherManager(JplootConfig config) {
		super();
		this.config = config;
	}

	@Override
	public void addLaunchers(JplootApplication application) {
		try {
			for (String launcher : application.launchers().orElse(new HashSet<>())) {
				String launcherContent = readLauncherToString();
				launcherContent = launcherContent.replace(
						"[[APPLICATION]]",
						String.format("%s:%s:%s",
								application.groupId(), application.artifactId(), application.version()));
				Path path = config.jplootHome().resolve("bin").resolve(launcher);
				Files.writeString(path, launcherContent,
						StandardOpenOption.CREATE_NEW);
				Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
				LOGGER.trace("Added launcher {}.", path);
			}
		} catch (IOException | RuntimeException e) {
			throw new InstallException("Error installing launchers.", e);
		}
	}

	@Override
	public void removeLaunchers(JplootApplication application) {
		try {
			for (String launcher : application.launchers().orElse(new HashSet<>())) {
				Path path = config.jplootHome().resolve("bin").resolve(launcher);
				if (!Files.deleteIfExists(path)) {
					LOGGER.warn("Launcher {} cannot be removed.", path);
				}
				LOGGER.trace("Removed launcher {}.", path);
			}
		} catch (IOException | RuntimeException e) {
			throw new InstallException("Error removing launchers.", e);
		}
	}

	private static String readLauncherToString() throws IOException {
		try (InputStream is = JplootLauncherManager.class.getResourceAsStream("/jploot/scripts/launcher");
				InputStreamReader reader = new InputStreamReader(is);
				StringWriter writer = new StringWriter()) {
			reader.transferTo(writer);
			return writer.toString();
		}
	}
}
