package jploot.api;

import jploot.config.model.JplootApplication;

public interface IJplootLauncherManager {

	public void addLaunchers(JplootApplication application);

	void removeLaunchers(JplootApplication application);

}
