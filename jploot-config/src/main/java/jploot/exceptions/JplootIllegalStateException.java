package jploot.exceptions;

public class JplootIllegalStateException extends RuntimeException {

	private static final long serialVersionUID = -3325515471901522148L;

	public JplootIllegalStateException() {
		super();
	}

	public JplootIllegalStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public JplootIllegalStateException(String message, Throwable cause) {
		super(message, cause);
	}

	public JplootIllegalStateException(String message) {
		super(message);
	}

	public JplootIllegalStateException(Throwable cause) {
		super(cause);
	}

}
