package jploot.api;

import java.nio.file.Path;

import jploot.config.model.JplootConfig;

public interface IConfigLoader {

	JplootConfig load(Path config);

}
