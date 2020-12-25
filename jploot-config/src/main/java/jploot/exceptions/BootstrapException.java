package jploot.exceptions;

public class BootstrapException extends JplootException {

	private static final long serialVersionUID = -6924445997274572712L;

	public BootstrapException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BootstrapException(String message, Throwable cause) {
		super(message, cause);
	}

	public BootstrapException(String message) {
		super(message);
	}

	public BootstrapException(Throwable cause) {
		super(cause);
	}

}
