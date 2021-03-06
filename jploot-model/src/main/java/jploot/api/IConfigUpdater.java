package jploot.api;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;

public interface IConfigUpdater {

	JplootConfig addApplication(JplootApplication application);

	JplootConfig removeApplication(JplootApplication application);

	JplootConfig addRepository(String url);

	JplootConfig removeRepository(String url);

}
