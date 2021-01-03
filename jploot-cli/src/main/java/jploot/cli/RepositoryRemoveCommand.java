package jploot.cli;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jploot.exceptions.RepositoryException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
		name = "remove",
		mixinStandardHelpOptions = true,
		description = "Remove a repository.")
public class RepositoryRemoveCommand extends AbstractCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryRemoveCommand.class);

	@Parameters(index = "0", arity = "1",
			description = "A Maven repository URL to remove.")
	private String url;

	@Override
	protected Integer doCall() {
		URI uri;
		try {
			uri = new URI(url);
			if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()) && !"file".equals(uri.getScheme())) {
				throw new RepositoryException("Repository protocol must be one of http|https|file");
			}
		} catch (RepositoryException re) {
			throw re;
		} catch (URISyntaxException | RuntimeException e) {
			throw new RepositoryException(String.format("Repository URL %s cannot be parsed", url), e);
		}
		if (!config().repositories().contains(uri)) {
			LOGGER.info("⭕ Repository <{}> already absent.", url);
			return 0;
		} else {
			configUpdater().removeRepository(url);
			LOGGER.info("📝 Repository <{}> removed.", url);
			return 0;
		}
	}

}
