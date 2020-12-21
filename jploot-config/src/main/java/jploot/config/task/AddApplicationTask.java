package jploot.config.task;

import jploot.config.model.JplootApplication;

public class AddApplicationTask {

	private final JplootApplication addedApplication;

	public AddApplicationTask(JplootApplication application) {
		this.addedApplication = application;
	}

	public JplootApplication getAddedApplication() {
		return addedApplication;
	}

}
