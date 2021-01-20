package jploot.config.loader;

import java.nio.file.Path;

import jploot.api.IConfigLoader;
import jploot.api.IFileLoader;
import jploot.config.model.JplootConfig;

/**
 * <p>
 * Load a jploot config file from a {@link ArgumentConfig}. Default configuration are applied in case configuration
 * file is incomplete.
 * </p>
 */
public class JplootConfigLoader extends AbstractJplootConfigHandling implements IConfigLoader {

	public JplootConfigLoader(IFileLoader fileLoader) {
		super(fileLoader);
	}

	@Override
	public JplootConfig load(Path config) {
		return super.load(config);
	}
}
