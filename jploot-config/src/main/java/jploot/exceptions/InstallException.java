package jploot.exceptions;

public class InstallException extends JplootException {

	private static final long serialVersionUID = -6924445997274572712L;

	public InstallException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InstallException(String message, Throwable cause) {
		super(message, cause);
	}

	public InstallException(String message) {
		super(message);
	}

	public InstallException(Throwable cause) {
		super(cause);
	}

}
