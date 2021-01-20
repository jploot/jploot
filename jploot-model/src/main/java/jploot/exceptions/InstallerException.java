package jploot.exceptions;

public class InstallerException extends JplootException {

	private static final long serialVersionUID = -6924445997274572712L;

	public InstallerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InstallerException(String message, Throwable cause) {
		super(message, cause);
	}

	public InstallerException(String message) {
		super(message);
	}

	public InstallerException(Throwable cause) {
		super(cause);
	}

}
