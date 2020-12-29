package jploot.exceptions;

public class JplootException extends RuntimeException {

	private static final long serialVersionUID = -3325515471901522148L;

	public JplootException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public JplootException(String message, Throwable cause) {
		super(message, cause);
	}

	public JplootException(String message) {
		super(message);
	}

	public JplootException(Throwable cause) {
		super(cause);
	}

}
