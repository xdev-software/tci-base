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
package software.xdev.tci.network;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.ResourceReaper;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkCmd;


/**
 * A better implementation of {@link Network} in relation to {@link org.testcontainers.containers.Network.NetworkImpl}.
 * <p>
 * Improvements:
 *     <ul>
 *         <li>Allows creation of the network in the background</li>
 *         <li>Doesn't create the network inside {@link NetworkImpl#getId()}</li>
 *         <li>Doesn't check for duplicate network names when using a random {@link UUID} as name (see
 *         {@link #checkDuplicate below})</li>
 *         <li>Tries to delete the network when it's closed</li>
 *     </ul>
 * </p>
 */
public class LazyNetwork implements Network
{
	private static final Logger LOG = LoggerFactory.getLogger(LazyNetwork.class);
	
	/**
	 * Behavior if <code>null</code>: random UUID will be chosen
	 */
	protected String name;
	protected Boolean enableIpv6;
	protected String driver;
	protected Set<Consumer<CreateNetworkCmd>> createNetworkCmdModifiers = new HashSet<>();
	/**
	 * Behavior if <code>null</code>:<br/>
	 * <code>true</code> when a {@link #name} was specified, otherwise <code>false</code> because
	 * <ul>
	 *     <li>When using a random UUIDv4 as name the chances of collision are extremely small (
	 *     <a href="https://en.wikipedia.org/wiki/Universally_unique_identifier#Collisions">1 : 2.17 x 10^18</a>
	 *     - you're 155 billion times more likely to win the lottery)
	 *     </li>
	 *     <li>According to the <a href="https://docs.docker.com/engine/api/v1.24/#create-a-network">Docker docs</a>
	 *     this is "best effort" and not guaranteed to catch all name collisions.
	 *     </li>
	 * </ul>
	 */
	protected Boolean checkDuplicate;
	
	protected boolean deleteNetworkOnClose = true;
	protected int deleteNetworkOnCloseTries = 20;
	
	protected CompletableFuture<Void> startCF;
	protected String id; // null -> not created
	
	public LazyNetwork create()
	{
		return this.create(CompletableFuture::runAsync);
	}
	
	public LazyNetwork create(final Function<Runnable, CompletableFuture<Void>> executor)
	{
		if(this.startCF != null)
		{
			throw new IllegalStateException("Creation was already started");
		}
		this.startCF = executor.apply(this::startInternal);
		return this;
	}
	
	@SuppressWarnings({"deprecation", "java:S1874", "java:S2095"})
	protected synchronized void startInternal()
	{
		if(this.id != null)
		{
			throw new IllegalStateException("Id was already set");
		}
		
		final CreateNetworkCmd createNetworkCmd = this.getClient().createNetworkCmd();
		
		if(this.checkDuplicate == null)
		{
			this.checkDuplicate = this.name != null;
		}
		if(this.name == null)
		{
			this.name = UUID.randomUUID().toString();
		}
		
		createNetworkCmd.withName(this.name);
		createNetworkCmd.withCheckDuplicate(this.checkDuplicate);
		
		if(this.enableIpv6 != null)
		{
			createNetworkCmd.withEnableIpv6(this.enableIpv6);
		}
		
		if(this.driver != null)
		{
			createNetworkCmd.withDriver(this.driver);
		}
		
		for(final Consumer<CreateNetworkCmd> consumer : this.createNetworkCmdModifiers)
		{
			consumer.accept(createNetworkCmd);
		}
		
		final Map<String, String> labels = Optional.ofNullable(createNetworkCmd.getLabels())
			.map(HashMap::new)
			.orElseGet(HashMap::new);
		labels.putAll(DockerClientFactory.DEFAULT_LABELS);
		labels.putAll(ResourceReaper.instance().getLabels());
		createNetworkCmd.withLabels(labels);
		
		this.id = createNetworkCmd.exec().getId();
		// Free up
		this.startCF = null;
	}
	
	public void waitForCreation(final Duration timeout)
	{
		if(this.id == null && this.startCF != null)
		{
			try
			{
				this.startCF.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			}
			catch(final InterruptedException iex)
			{
				Thread.currentThread().interrupt();
			}
			catch(final Exception ex)
			{
				throw new IllegalStateException("Unable to start", ex);
			}
		}
	}
	
	public String getIdWithoutCheck()
	{
		return this.id;
	}
	
	@Override
	public String getId()
	{
		this.waitForCreation(Duration.ofMinutes(10));
		
		return this.id;
	}
	
	@Override
	public void close()
	{
		if(this.id != null)
		{
			this.closeInternal();
		}
	}
	
	@SuppressWarnings("resource") // AutoClosable is implemented but does nothing?
	protected synchronized void closeInternal()
	{
		if(this.id != null)
		{
			if(this.deleteNetworkOnClose)
			{
				try
				{
					final AtomicInteger retryCounter = new AtomicInteger(1);
					// Docker needs a few moments until all endpoints are removed from the network
					Unreliables.retryUntilSuccess(
						this.deleteNetworkOnCloseTries,
						() -> {
							if(retryCounter.getAndIncrement() > 1)
							{
								Thread.sleep(1000);
							}
							return this.getClient().removeNetworkCmd(this.id).exec();
						}
					);
				}
				catch(final RuntimeException rex)
				{
					LOG.warn("Failed to delete network; May cause network leak", rex);
				}
			}
			this.id = null;
		}
	}
	
	protected DockerClient getClient()
	{
		return DockerClientFactory.instance().client();
	}
	
	// region Get/Set
	
	public LazyNetwork withName(final String name)
	{
		this.name = name;
		return this;
	}
	
	public LazyNetwork withEnableIpv6(final Boolean enableIpv6)
	{
		this.enableIpv6 = enableIpv6;
		return this;
	}
	
	public LazyNetwork withDriver(final String driver)
	{
		this.driver = driver;
		return this;
	}
	
	public LazyNetwork withCreateNetworkCmdModifier(final Consumer<CreateNetworkCmd> createNetworkCmdModifier)
	{
		this.createNetworkCmdModifiers.add(createNetworkCmdModifier);
		return this;
	}
	
	public LazyNetwork withCheckDuplicate(final Boolean checkDuplicate)
	{
		this.checkDuplicate = checkDuplicate;
		return this;
	}
	
	public LazyNetwork withDeleteNetworkOnClose(final boolean deleteNetworkOnClose)
	{
		this.deleteNetworkOnClose = deleteNetworkOnClose;
		return this;
	}
	
	public LazyNetwork withDeleteNetworkOnCloseTries(final int deleteNetworkOnCloseTries)
	{
		this.deleteNetworkOnCloseTries = deleteNetworkOnCloseTries;
		return this;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public Boolean getEnableIpv6()
	{
		return this.enableIpv6;
	}
	
	public String getDriver()
	{
		return this.driver;
	}
	
	public Set<Consumer<CreateNetworkCmd>> getCreateNetworkCmdModifiers()
	{
		return this.createNetworkCmdModifiers;
	}
	
	public Boolean getCheckDuplicate()
	{
		return this.checkDuplicate;
	}
	
	public boolean isDeleteNetworkOnClose()
	{
		return this.deleteNetworkOnClose;
	}
	
	public int getDeleteNetworkOnCloseTries()
	{
		return this.deleteNetworkOnCloseTries;
	}
	
	// endregion
	
	// region Legacy
	
	/**
	 * @deprecated JUNit4 is effectively dead
	 */
	@Deprecated(forRemoval = true)
	@Override
	public Statement apply(final Statement base, final Description description)
	{
		return null;
	}
	// endregion
}
