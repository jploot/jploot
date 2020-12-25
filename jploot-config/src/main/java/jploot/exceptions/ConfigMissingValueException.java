package jploot.exceptions;

@SuppressWarnings("java:S110") // inheritance limit
public class ConfigMissingValueException extends ConfigException {
	
	private static final long serialVersionUID = -8162741671486748445L;
	
	final String name;
	
	public ConfigMissingValueException(String name) {
		super(String.format("Configuration %s is missing", name));
		this.name = name;
	}
	public String getConfigName() {
		return name;
	}
}