package jploot.api;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;

public interface IJplootConfigUpdater {

	JplootConfig addApplication(JplootApplication application);

	JplootConfig removeApplication(JplootApplication application);

}
