package jploot.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.ParentCommand;

public abstract class AbstractCommand implements Callable<Integer> {

	@ParentCommand
	private JplootMain jploot;

	public JplootMain parent() {
		return jploot;
	}

}
