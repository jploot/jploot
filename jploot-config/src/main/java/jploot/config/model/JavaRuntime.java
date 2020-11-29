package jploot.config.model;

import java.nio.file.Path;

import org.immutables.value.Value;

@Value.Immutable
public interface JavaRuntime {

	String name();
	Path javaHome();
	String version();

}
