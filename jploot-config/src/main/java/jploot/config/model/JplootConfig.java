package jploot.config.model;

import java.nio.file.Path;
import java.util.Set;

import org.immutables.value.Value;

@Value.Immutable
public interface JplootConfig {

	Path location();
	@Value.Auxiliary
	Set<JavaRuntime> runtimes();
	@Value.Auxiliary
	Set<JplootApplication> applications();

	default Path jplootBase() {
		return location().getParent();
	}

}
