package software.xdev.tci.safestart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;


/**
 * It's not possible to start a container with e.g. the same name, port bindings, etc. with <code>startupAttempts >
 * 1</code>, which is the default in a few containers (e.g. MariaDB or Selenium-Browsers).
 * <p/>
 * {@link SafeNamedContainerStarter} addresses these problems by
 * <ul>
 *     <li>ensuring that after every started container has a unique name (addresses the name problem)</li>
 *     <li>removing previously failed started containers (addresses the port and other problems)</li>
 * </ul>
 */
public class SafeNamedContainerStarter<C extends GenericContainer<?>> implements Runnable
{
	protected static final Logger LOG = LoggerFactory.getLogger(SafeNamedContainerStarter.class);
	protected final String baseContainerName;
	protected final List<String> containerNames = Collections.synchronizedList(new ArrayList<>());
	protected final C container;
	protected final Consumer<C> starter;
	
	protected boolean attachRandomUUID = true;
	
	public SafeNamedContainerStarter(
		final String baseContainerName,
		final C container)
	{
		this(baseContainerName, container, C::start);
	}
	
	public SafeNamedContainerStarter(
		final String baseContainerName,
		final C container,
		final Consumer<C> starter)
	{
		this.baseContainerName = Objects.requireNonNull(baseContainerName);
		this.container = Objects.requireNonNull(container);
		this.starter = Objects.requireNonNull(starter);
		
		this.prepareContainer();
	}
	
	void prepareContainer()
	{
		final AtomicInteger startAttemptCounter = new AtomicInteger(1);
		this.container.withCreateContainerCmdModifier(cmd ->
		{
			// We are here again -> Prev start (if any) must have failed!
			// Clear now, otherwise e.g. fixed port bindings will fail again and forever
			this.tryCleanupContainerAfterStartFail(this.containerNames);
			this.containerNames.clear();
			
			// Generate new name on each start attempt, so that we can clearly identify the container
			String generatedName = this.baseContainerName + "-" + startAttemptCounter.getAndIncrement();
			if(this.attachRandomUUID)
			{
				generatedName += "-" + UUID.randomUUID();
			}
			// Add first so that we can skip the stream later (there is no skipLast Method)
			this.containerNames.addFirst(generatedName);
			
			cmd.withName(generatedName);
		});
	}
	
	public SafeNamedContainerStarter<C> withAttachRandomUUID(final boolean attachRandomUUID)
	{
		this.attachRandomUUID = attachRandomUUID;
		return this;
	}
	
	@Override
	public void run()
	{
		this.start();
	}
	
	public void start()
	{
		try
		{
			this.starter.accept(this.container);
			this.tryCleanupContainerAfterStartFail(this.containerNames.stream()
				.skip(1) // First one is successfully started one
				.toList());
		}
		catch(final RuntimeException rex)
		{
			this.tryCleanupContainerAfterStartFail(this.containerNames);
			throw rex;
		}
	}
	
	@SuppressWarnings("resource")
	protected void tryCleanupContainerAfterStartFail(final List<String> containerNames)
	{
		for(final String containerName : containerNames)
		{
			LOG.info("Start of container[name='{}'] failed; Trying to remove container...", containerName);
			try
			{
				DockerClientFactory.lazyClient()
					.removeContainerCmd(containerName)
					.withForce(true)
					.exec();
				LOG.info("Removed failed container[name='{}']", containerName);
			}
			catch(final Exception ex)
			{
				LOG.debug("Unable to cleanup container[name='{}']", containerName, ex);
			}
		}
	}
}
