package jploot.cli;

import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.Callable;

import jploot.api.IConfigUpdater;
import jploot.api.ILauncherManager;
import jploot.api.IRepositoryUpdater;
import jploot.config.loader.FileLoader;
import jploot.config.loader.JplootConfigUpdater;
import jploot.config.loader.JplootLauncherManager;
import jploot.config.loader.JplootRepositoryUpdater;
import jploot.config.model.JplootApplication;
import jploot.config.model.JplootConfig;
import jploot.config.model.JplootDependency;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

public abstract class AbstractCommand implements Callable<Integer> {

	@ParentCommand
	private Object parent;

	@Spec
	CommandSpec spec;

	@Override
	public final Integer call() {
		jploot().init(needsConfig());
		return doCall();
	}

	boolean needsConfig() {
		return true;
	}

	private Object parent() {
		return parent;
	}

	public JplootMain jploot() {
		Object currentParent = parent();
		while (currentParent instanceof AbstractCommand) {
			currentParent = ((AbstractCommand) currentParent).parent();
		}
		return (JplootMain) currentParent;
	}

	protected PrintStream out() {
		return jploot().out;
	}

	protected PrintStream err() {
		return jploot().err;
	}

	protected JplootConfig config() {
		return jploot().jplootConfig;
	}

	protected IConfigUpdater configUpdater() {
		return new JplootConfigUpdater(new FileLoader(), config());
	}

	protected ILauncherManager launcherManager() {
		return new JplootLauncherManager(config());
	}

	protected IRepositoryUpdater repositoryUpdater() {
		return new JplootRepositoryUpdater(new FileLoader(), config());
	}

	protected abstract Integer doCall();

	protected Optional<JplootApplication> findApplication(JplootDependency application) {
		return config().applications().stream().filter(c -> match(c, application)).findFirst();
	}

	protected Optional<JplootApplication> findApplication(String nameSpec) {
		return config().applications().stream().filter(c -> match(c, nameSpec)).findFirst();
	}

	private boolean match(JplootApplication candidate, JplootDependency application) {
		return candidate.groupId().equals(application.groupId())
				&& candidate.artifactId().equals(application.artifactId())
				&& candidate.version().equals(application.version());
	}

	private boolean match(JplootApplication candidate, String nameSpec) {
		if (nameSpec.contains(":")) {
			String[] nameSpecList = nameSpec.split(":");
			if (nameSpecList.length == 2) {
				return candidate.name().equals(nameSpecList[0])
						&& candidate.version().equals(nameSpecList[1]);
			} else {
				return candidate.groupId().equals(nameSpecList[0])
						&& candidate.artifactId().equals(nameSpecList[1])
						&& candidate.version().equals(nameSpecList[2]);
			}
		} else {
			return candidate.name().equals(nameSpec);
		}
	}

}
