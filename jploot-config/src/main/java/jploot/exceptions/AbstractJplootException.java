package jploot.exceptions;

import jploot.config.model.JplootConfig;

public class AbstractJplootException extends Exception {

	private static final long serialVersionUID = -6084207865789316051L;

	final JplootConfig config;

	public AbstractJplootException(String message, JplootConfig config, Throwable e) {
		super(message, e);
		this.config = config;
	}

}
