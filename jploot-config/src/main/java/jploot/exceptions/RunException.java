package jploot.exceptions;

public class RunException extends JplootException {

	private static final long serialVersionUID = -6924445997274572712L;

	public RunException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public RunException(String message, Throwable cause) {
		super(message, cause);
	}

	public RunException(String message) {
		super(message);
	}

	public RunException(Throwable cause) {
		super(cause);
	}

}
