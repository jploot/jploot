package jploot.api;

import java.nio.file.Path;

import jploot.config.model.JplootConfig;

public interface IJplootConfigLoader {

	JplootConfig load(Path config);

}
