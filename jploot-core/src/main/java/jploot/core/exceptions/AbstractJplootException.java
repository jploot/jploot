package jploot.core.exceptions;

import jploot.config.model.JplootBase;
import jploot.config.model.JplootConfig;

public class AbstractJplootException extends Exception {

	private static final long serialVersionUID = -6084207865789316051L;

	final JplootConfig config;
	final JplootBase jplootBase;

	public AbstractJplootException(String message, JplootConfig config, JplootBase jplootBase, Throwable e) {
		super(message, e);
		this.config = config;
		this.jplootBase = jplootBase;
	}

}
