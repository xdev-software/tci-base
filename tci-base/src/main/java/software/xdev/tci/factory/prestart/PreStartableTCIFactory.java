/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.tci.factory.prestart;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.model.ContainerNetwork;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.BaseTCIFactory;
import software.xdev.tci.factory.prestart.config.PreStartConfig;
import software.xdev.tci.factory.prestart.coordinator.GlobalPreStartCoordinator;
import software.xdev.tci.portfixation.PortFixation;


/**
 * A PreStarting-able implementation of {@link software.xdev.tci.factory.TCIFactory}.
 * <h3>What is PreStarting?</h3>
 * When running tests usually there are certain times when the available resources are barely utilized.
 * <p>
 * PreStarting uses a "cached" pool of infrastructure and tries to utilizes these idle times to fill/replenish this
 * pool.<br/> So that when new infrastructure is requested there is no need to wait for the creation of it and use the
 * already started infrastructure from this pool - if it's available.
 * </p>
 * <h3>Requirements</h3>
 * Infrastructure needs to be <u>dependency- and stateless</u>:
 * <p>This means that e.g. a container can be started without relying on another container.<br/> If another
 * infrastructure/container is needed when doing e.g. a certain request during testing it's
 * advised to use DNS names inside the initial configuration.<br/>The infrastructure may also be configured in a way
 * that test specific data can be created before the test e.g. using a client and not during PreStarting.
 * </p>
 * <p>
 * <b>Important: PreStarting is disabled by default!</b> So when executing test manually, one can solely focus on
 * the test.<br/> For more information have a look at {@link PreStartConfig} and it's implementation(s).
 * </p>
 * <h3>In which situation has this the greatest advantage?</h3>
 * and "Why not just use
 * <a href="https://junit.org/junit5/docs/snapshot/user-guide/#writing-tests-parallel-execution">parallel</a> test
 * execution?"
 * <ul>
 *     <li>Sometimes parallel test execution is not possible</li>
 *     <li>When starts of containers/infrastructure and/or test execution takes a long time and uses not that many
 *     resources.
 *     <br/>Note however that this is highly situational and depends on used hardware and infrastructure</li>
 *     <li>
 *         The general design principle of <u>dependency- and statelessness</u> allows multiple containers to be
 *         started in parallel during test initialization even WITHOUT enabled PreStarting.<br/>Example:
 *         <pre>
 *  // Szenario 1: Conventionally starting infrastructures
 *  dbInfra = DB_INFRA_FACTORY.getNew(network, DNS_NAME_DB); // Starts in 10s
 *  authInfra = AUTH_INFRA_FACTORY.getNew(network, DNS_NAME_AUTH); // Starts in 10s
 *
 *  appInfra = APP_INFRA_FACTORY.getNew(network, DNS_NAME_APP); // Starts in 20s
 *
 *  // Maximum total start time would be ~40s
 *
 *
 *  // Szenario 2 (better): Starting infrastructures in parallel
 *  var cfDBInfra = CompletableFuture.supplyAsync(() -> DB_INFRA_FACTORY.getNew(network, DNS_NAME_DB));
 *  var cfAuthInfra = CompletableFuture.supplyAsync(() -> AUTH_INFRA_FACTORY.getNew(network, DNS_NAME_AUTH));
 *
 *  appInfra = APP_INFRA_FACTORY.getNew(network, DNS_NAME_APP);
 *
 *  dbInfra = cfDBInfra.join();
 *  authInfra = cfAuthInfra.join();
 *
 *  // Maximum total start time would be ~20s
 *         </pre>
 *     </li>
 * </ul>
 * Therefore, it's highly recommended to try multiple options.<br/> E.g. enabling/disabling PreStarting and using a
 * different amount of JUnit parallelization.
 * <h3>Caveats of PreStarting</h3>
 * Currently, PreStarting has the following trade-offs:
 * <ul>
 *     <li>As PreStarted containers/infrastructure are kept in a "cache" pool additional RAM/memory is required</li>
 *     <li>
 *         To connect PreStarted containers to a network where they can communicate with each other
 *         <code>docker network connect</code> is used. This command is not quite optimal:
 *         <ul>
 *             <li>Due to a bug the host-ports of the container must be fixated. See {@link PortFixation}</li>
 *             <li>When lots of containers and networks are active the command can get quite slow and may
 *             needs a few seconds to execute</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public class PreStartableTCIFactory<C extends GenericContainer<C>, I extends TCI<C>>
	extends BaseTCIFactory<C, I>
{
	/**
	 * Name (used for Thread-names and Logging)
	 */
	protected final String name;
	/**
	 * Has the following effects:
	 * <ul>
	 *     <li><code>true</code> (default) - Directly attaches the Container to the network during startup if
	 *     possible</li>
	 *     <li><code>false</code> - Performs a Network#connect as if PreStarting is active.
	 *     This is slower however it emulates PreStarting better and may help finding bugs.</li>
	 * </ul>
	 */
	protected final boolean useDirectNetworkAttachIfPossible;
	
	// endregion
	protected final LinkedBlockingQueue<StartingInfra<I>> preStartQueue;
	
	protected final AtomicInteger nextThreadId = new AtomicInteger(1);
	
	protected final ThreadPoolExecutor executorService;
	protected final AtomicInteger preStartCounter = new AtomicInteger(1);
	
	protected final Timeouts timeouts;
	
	public PreStartableTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName,
		final String name)
	{
		this(
			infraBuilder,
			containerBuilder,
			containerBaseName,
			containerLoggerName,
			name,
			PreStartConfig.instance(),
			new Timeouts()
		);
	}
	
	public PreStartableTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName,
		final String name,
		final PreStartConfig config,
		final Timeouts timeouts
	)
	{
		super(infraBuilder, containerBuilder, containerBaseName, containerLoggerName);
		
		this.name = Objects.requireNonNull(name);
		
		final int amountToKeepReady = config.keepReady(name);
		this.preStartQueue = amountToKeepReady > 0 ? new LinkedBlockingQueue<>(amountToKeepReady) : null;
		
		this.useDirectNetworkAttachIfPossible = config.directNetworkAttachIfPossible(name);
		
		final int maxAmountStartingSimultaneously = config.maxStartSimultan(name);
		
		final ThreadFactory threadFactory = r ->
		{
			final Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("InfraPreStarter-" + this.name + "-" + this.nextThreadId.getAndIncrement());
			return t;
		};
		this.executorService = maxAmountStartingSimultaneously < 0
			? (ThreadPoolExecutor)Executors.newCachedThreadPool(threadFactory)
			: (ThreadPoolExecutor)Executors.newFixedThreadPool(maxAmountStartingSimultaneously, threadFactory);
		
		this.timeouts = Objects.requireNonNull(timeouts);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void warmUpInternal()
	{
		if(this.isPreStartingDisabled())
		{
			return;
		}
		
		GlobalPreStartCoordinator.instance().register(this);
	}
	
	public void schedulePreStart()
	{
		if(this.preStartQueue == null)
		{
			return;
		}
		
		this.preStartQueue.removeIf(preStarted -> preStarted.startFuture().isCompletedExceptionally());
		if(this.preStartQueue.remainingCapacity() > 0)
		{
			this.preStartQueue.add(this.bootNew(null, true));
		}
	}
	
	protected StartingInfra<I> bootNew(final DirectNetworkAttachInfo directAttachNetwork)
	{
		return this.bootNew(directAttachNetwork, false);
	}
	
	protected StartingInfra<I> bootNew(
		final DirectNetworkAttachInfo directAttachNetwork,
		final boolean preStarted)
	{
		this.log().info("[{}] Booting new infra", this.name);
		
		final long startTime = System.currentTimeMillis();
		
		try
		{
			final C container = this.buildContainer();
			Optional.ofNullable(directAttachNetwork)
				.ifPresent(
					// Add to network + aliases
					i -> container.withNetwork(i.network())
						.withNetworkAliases(i.aliases().toArray(String[]::new)));
			
			final I infra = this.infraBuilder.apply(container, null);
			
			return new StartingInfra<>(
				infra,
				CompletableFuture.runAsync(() -> {
					final long startTimeInfra = System.currentTimeMillis();
					try
					{
						// Fix ports for network attach later
						if(directAttachNetwork == null)
						{
							PortFixation.makeExposedPortsFix(container);
						}
						infra.start(this.containerBaseName
							+ "-"
							+ this.preStartCounter.getAndIncrement()
							+ (preStarted ? "-PS" : ""));
					}
					finally
					{
						this.tracer.timedAdd("infraStart(async)", System.currentTimeMillis() - startTimeInfra);
					}
				}, this.executorService),
				directAttachNetwork == null);
		}
		finally
		{
			this.tracer.timedAdd("bootNew", System.currentTimeMillis() - startTime);
		}
	}
	
	protected StartingInfra<I> acquireNew(final DirectNetworkAttachInfo directAttachNetwork)
	{
		this.log().info("[{}] Getting a new infra; Timeout={}", this.name, this.timeouts.getAcquireTimeout());
		final long startTime = System.currentTimeMillis();
		
		final StartingInfra<I> startingInfra =
			this.isPreStartingDisabled()
				? this.bootNew(directAttachNetwork)
				// Try to use preStarting from queue or else boot a new one
				: Optional.ofNullable(this.preStartQueue.poll())
				.orElseGet(() -> this.bootNew(directAttachNetwork));
		
		try
		{
			startingInfra.startFuture().get(this.timeouts.getAcquireTimeout().toMillis(), TimeUnit.MILLISECONDS);
		}
		catch(final InterruptedException ie)
		{
			this.handleInterrupt(ie);
		}
		catch(final Exception e)
		{
			// Try to clean up
			this.handleInfraStartFail(startingInfra.infra());
			throw new IllegalStateException("Unable to start infra", e);
		}
		finally
		{
			if(this.log().isInfoEnabled())
			{
				this.log().info(
					"[{}] Finished waiting for infra, took {}ms",
					this.name,
					System.currentTimeMillis() - startTime);
			}
		}
		
		if(!startingInfra.infra().getContainer().isRunning())
		{
			throw new IllegalStateException("Container is not running! " + startingInfra.infra().getContainer());
		}
		
		return startingInfra;
	}
	
	protected I newInternal(final Network network, final String... aliases)
	{
		final List<String> filteredAliases = Stream.of(aliases)
			.filter(Objects::nonNull)
			.toList();
		
		final StartingInfra<I> startingInfra = this.acquireNew(this.useDirectNetworkAttachIfPossible && network != null
			? new DirectNetworkAttachInfo(network, filteredAliases)
			: null);
		final I infra = startingInfra.infra();
		
		try
		{
			if(network != null && startingInfra.requiresNetworkConnect())
			{
				final long connectToNetworkStartTime = System.currentTimeMillis();
				
				this.connectContainerToNetwork(infra.getContainer(), network, filteredAliases);
				
				this.tracer.timedAdd("connectToNetwork", System.currentTimeMillis() - connectToNetworkStartTime);
			}
			
			filteredAliases.stream()
				.findFirst()
				.ifPresent(infra::setNetworkAlias);
		}
		catch(final RuntimeException rex)
		{
			this.handleInfraStartFail(infra);
			throw rex;
		}
		
		return infra;
	}
	
	protected void connectContainerToNetwork(
		final GenericContainer<?> container,
		final Network network,
		final List<String> aliases)
	{
		@SuppressWarnings("resource") // lazy-client is singleton -> if closed nothing works anymore
		final ConnectToNetworkCmd connectToNetworkCmd = DockerClientFactory.lazyClient()
			.connectToNetworkCmd()
			.withNetworkId(network.getId())
			.withContainerId(container.getContainerId());
		if(!aliases.isEmpty())
		{
			connectToNetworkCmd.withContainerNetwork(new ContainerNetwork().withAliases(aliases));
		}
		
		try
		{
			CompletableFuture.runAsync(connectToNetworkCmd::exec)
				.get(this.timeouts.getConnectToNetworkTimeout().toMillis(), TimeUnit.MILLISECONDS);
		}
		catch(final InterruptedException ie)
		{
			this.handleInterrupt(ie);
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Unable to connect container[" + container + "] to network[" + network + "]",
				e);
		}
	}
	
	public I getNew(final Network network, final String... aliases)
	{
		this.warmUp();
		
		this.log().info("Getting new infra");
		final long startTime = System.currentTimeMillis();
		
		final I infra = this.registerReturned(Unreliables.retryUntilSuccess(
			this.getNewTryCount,
			() -> this.newInternal(network, aliases)));
		
		final long startTimePostProcess = System.currentTimeMillis();
		this.postProcessNew(infra);
		this.tracer.timedAdd("postProcessNew", System.currentTimeMillis() - startTimePostProcess);
		
		final long ms = System.currentTimeMillis() - startTime;
		this.log().info("Got new infra, took {}ms", ms);
		
		this.tracer.timedAdd("getNew", ms);
		
		return infra;
	}
	
	/**
	 * This method can be used for post-processing after new infra was acquired.
	 * <p>
	 * Example:<br/> Docker needs a few milliseconds (usually less than 100) to reconfigure its networks.<br/> In the
	 * meantime existing connections might fail.<br/> This method can be used to validate these connections.
	 * </p>
	 */
	protected void postProcessNew(final I infra)
	{
		// NO OP
	}
	
	protected boolean isPreStartingDisabled()
	{
		return this.preStartQueue == null;
	}
	
	@SuppressWarnings("resource")
	@Override
	public void close()
	{
		this.log().warn("[{}] Shutting down", this.name);
		if(!this.isPreStartingDisabled())
		{
			GlobalPreStartCoordinator.instance().unregister(this);
		}
		this.executorService.shutdown();
		final List<CompletableFuture<Void>> stopCFs = this.preStartQueue.stream()
			.map(i -> CompletableFuture.runAsync(() ->
			{
				try
				{
					i.infra().stop();
				}
				catch(final Exception e)
				{
					this.log().warn("[{}] Failed to shutdown infra", this.name, e);
				}
			}))
			.toList();
		stopCFs.forEach(CompletableFuture::join);
		// De-Ref for GC
		this.preStartQueue.clear();
		
		super.close();
	}
	
	// region Utility
	protected void handleInterrupt(final InterruptedException ie)
	{
		this.log().warn("[{}] Got interrupted", this.name, ie);
		Thread.currentThread().interrupt();
	}
	
	protected record StartingInfra<I>(
		I infra,
		CompletableFuture<Void> startFuture,
		boolean requiresNetworkConnect)
	{
		public StartingInfra
		{
			Objects.requireNonNull(infra);
			Objects.requireNonNull(startFuture);
		}
	}
	
	
	protected record DirectNetworkAttachInfo(Network network, List<String> aliases)
	{
		public DirectNetworkAttachInfo
		{
			Objects.requireNonNull(network);
		}
	}
	
	
	public static class Timeouts
	{
		private Duration acquireTimeout = Duration.ofMinutes(3);
		private Duration connectToNetworkTimeout = Duration.ofMinutes(3);
		
		public Timeouts withAcquireTimeout(final Duration acquireTimeout)
		{
			this.acquireTimeout = acquireTimeout;
			return this;
		}
		
		public Timeouts withConnectToNetworkTimeout(final Duration connectToNetworkTimeout)
		{
			this.connectToNetworkTimeout = connectToNetworkTimeout;
			return this;
		}
		
		public Duration getAcquireTimeout()
		{
			return this.acquireTimeout;
		}
		
		public Duration getConnectToNetworkTimeout()
		{
			return this.connectToNetworkTimeout;
		}
	}
	// endregion
}
