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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.concurrent.ExecutorServiceCreatorHolder;
import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.factory.prestart.config.PreStartConfig;


/**
 * Provides a pool of {@link LazyNetwork}
 */
public class LazyNetworkPool implements AutoCloseable
{
	private static final Logger LOG = LoggerFactory.getLogger(LazyNetworkPool.class);
	
	protected static final AtomicInteger POOL_COUNTER = new AtomicInteger(1);
	
	protected static final AtomicInteger NETWORK_COUNTER = new AtomicInteger(1);
	
	@SuppressWarnings("resource")
	protected Supplier<LazyNetwork> networkSupplier = () -> new LazyNetwork()
		.withName("lazynet-" + NETWORK_COUNTER.getAndIncrement() + "-" + UUID.randomUUID())
		.withCheckDuplicate(false);
	
	protected final LinkedBlockingQueue<LazyNetwork> queue;
	protected final ExecutorService executor;
	
	/**
	 * Like {@link #LazyNetworkPool(int)} but uses {@link PreStartConfig} to determine the size
	 */
	public LazyNetworkPool()
	{
		this(PreStartConfig.instance().keepReady("lazynetwork"));
	}
	
	public LazyNetworkPool(final int size)
	{
		this(
			size,
			ExecutorServiceCreatorHolder.instance().createUnlimited(
				"LazyNetworkPool-" + POOL_COUNTER.getAndIncrement()));
	}
	
	public LazyNetworkPool(final int size, final ExecutorService executor)
	{
		this.queue = size > 0 ? new LinkedBlockingQueue<>(size) : null;
		this.executor = executor;
	}
	
	public LazyNetworkPool withNetworkSupplier(final Supplier<LazyNetwork> networkSupplier)
	{
		this.networkSupplier = networkSupplier;
		return this;
	}
	
	protected LazyNetwork bootNew()
	{
		final LazyNetwork network = this.networkSupplier.get();
		network.create(r -> CompletableFuture.runAsync(r, this.executor));
		return network;
	}
	
	public void managePoolAsync()
	{
		if(this.queue == null)
		{
			return;
		}
		
		CompletableFuture.runAsync(this::managePoolInternal, TCIExecutorServiceHolder.instance());
	}
	
	protected synchronized void managePoolInternal()
	{
		try
		{
			while(this.queue.remainingCapacity() > 0)
			{
				this.queue.add(this.bootNew());
			}
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to replenish pool", ex);
		}
	}
	
	public LazyNetwork getNew()
	{
		if(this.queue == null)
		{
			return this.bootNew();
		}
		
		final LazyNetwork net = Optional.ofNullable(this.queue.poll()).orElseGet(this::bootNew);
		
		// Replenish
		this.managePoolAsync();
		
		return net;
	}
	
	public void shutdown(final boolean drainQueueAndCloseNetworks)
	{
		this.executor.shutdown();
		
		if(drainQueueAndCloseNetworks && this.queue != null)
		{
			for(final LazyNetwork network : this.queue)
			{
				try
				{
					network.close();
				}
				catch(final Exception ex)
				{
					LOG.warn("Failed to close network", ex);
				}
			}
		}
	}
	
	@Override
	public void close()
	{
		// false because Networks will be deleted by Testcontainers ryuk anyway
		this.shutdown(false);
	}
}
