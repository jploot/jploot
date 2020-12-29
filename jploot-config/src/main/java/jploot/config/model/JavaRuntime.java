package jploot.config.model;

import java.nio.file.Path;

import org.immutables.value.Value;

/**
 * A JVM installation
 */
@Value.Immutable
public interface JavaRuntime {

	/**
	 * A name to identify this runtime.
	 */
	String name();
	/**
	 * JAVA_HOME path.
	 */
	Path javaHome();
	/**
	 * The JVM major version
	 */
	// TODO: use an enum
	// TODO: add an attribute for full version
	String version();

}
