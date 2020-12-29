package jploot.config.loader;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jploot.api.IJplootConfigUpdater;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.config.model.yaml.ImmutableJplootConfigFile;
import jploot.config.model.yaml.JplootApplicationFile;
import jploot.config.model.yaml.JplootConfigFile;
import jploot.exceptions.JplootException;

/**
 * <p>
 * Control configuration updates.
 * </p>
 */
public class JplootConfigUpdater extends AbstractJplootConfigHandling implements IJplootConfigUpdater {

	JplootConfig config;

	public JplootConfigUpdater(FileLoader fileLoader, JplootConfig config) {
		super(fileLoader);
		this.config = config;
	}

	@Override
	public JplootConfig addApplication(JplootApplication application) {
		return updateApplications(l -> l.add(applicationFile(application)));
	}

	@Override
	public JplootConfig removeApplication(JplootApplication application) {
		return updateApplications(l -> l.remove(applicationFile(application)));
	}

	private JplootConfig updateApplications(Consumer<Set<JplootApplicationFile>> updater) {
		JplootConfigFile currentConfig = loadJplootConfigFile(config.location());
		Optional<Set<JplootApplicationFile>> currentApplications = currentConfig.applications();
		Set<JplootApplicationFile> newApplications = new HashSet<>();
		if (currentApplications.isPresent()) {
			newApplications.addAll(currentApplications.get());
		}
		updater.accept(newApplications);
		return updateConfig(currentConfig, b -> b.applications(newApplications));
	}

	private JplootConfig updateConfig(JplootConfigFile currentConfig,
			Consumer<ImmutableJplootConfigFile.Builder> updater) {
		ImmutableJplootConfigFile.Builder builder = new ImmutableJplootConfigFile.Builder()
				.from(currentConfig);
		updater.accept(builder);
		JplootConfigFile newConfig = builder.build();
		try {
			String newConfigContent = mapper.writeValueAsString(newConfig);
			try (Writer writer = new FileWriter(config.location().toFile())) {
				writer.write(newConfigContent);
			}
			return load(config.location());
		} catch (IOException e) {
			throw new JplootException("Error saving configuration", e);
		}
	}
}
