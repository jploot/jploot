package jploot.config.exceptions;

import jploot.config.model.JplootApplication;
import jploot.config.model.JplootBase;
import jploot.config.model.JplootConfig;

public class AbstractJplootApplicationException extends AbstractJplootException {

	private static final long serialVersionUID = -7189506564079940994L;

	private final JplootApplication application;

	public AbstractJplootApplicationException(String message, JplootApplication application, JplootConfig config,
			JplootBase jplootBase, Throwable e) {
		super(message, config, jplootBase, e);
		this.application = application;
	}

}
