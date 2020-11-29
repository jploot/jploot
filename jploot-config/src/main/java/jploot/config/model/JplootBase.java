package jploot.config.model;

import java.nio.file.Path;
import java.util.List;

import org.immutables.value.Value;

@Value.Immutable
public interface JplootBase {

	String name();
	Path location();
	// TODO @Value.Auxiliary ?
	@Value.Redacted
	List<JavaRuntime> javaRuntimes();

}
