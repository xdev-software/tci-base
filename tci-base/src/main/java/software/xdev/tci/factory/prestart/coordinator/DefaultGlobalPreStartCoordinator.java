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
package software.xdev.tci.factory.prestart.coordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.factory.prestart.PreStartableTCIFactory;
import software.xdev.tci.factory.prestart.config.PreStartConfig;
import software.xdev.tci.factory.prestart.loadbalancing.LoadMonitor;


/**
 * Default implementation of {@link GlobalPreStartCoordinator}.
 * <p>
 * Coordinates PreStarting by monitoring the current resource (e.g. CPU) usage of the container environment.
 * </p>
 */
public class DefaultGlobalPreStartCoordinator implements GlobalPreStartCoordinator
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultGlobalPreStartCoordinator.class);
	
	protected final ScheduledExecutorService preStartScheduler;
	protected final List<PreStartableTCIFactory<?, ?>> factories = Collections.synchronizedList(new ArrayList<>());
	// Sadly there is no Java Set with an index or a unique list
	// So we have to keep the factories in order inside a List and ensure the uniqueness with a Set
	protected final Set<PreStartableTCIFactory<?, ?>> factoriesWeakSet =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	protected final AtomicInteger counter = new AtomicInteger(0);
	
	public DefaultGlobalPreStartCoordinator()
	{
		this.preStartScheduler = Executors.newSingleThreadScheduledExecutor(r ->
		{
			final Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("Global-InfraPreStarter-Scheduler");
			return t;
		});
		this.preStartScheduler.scheduleAtFixedRate(
			this::schedulePreStart,
			0,
			PreStartConfig.instance().coordinatorSchedulePeriodMs(),
			TimeUnit.MILLISECONDS);
		
		LOG.info("Started");
	}
	
	@SuppressWarnings("PMD.AvoidSynchronizedStatement") // Required by synchronizedList
	private void schedulePreStart()
	{
		try
		{
			if(LoadMonitor.instance().getCurrentIdlePercent().orElse(100)
				> PreStartConfig.instance().coordinatorIdleCPUPercent())
			{
				final PreStartableTCIFactory<?, ?> factory;
				synchronized(this.factories)
				{
					factory = this.factories.get(this.counter.getAndIncrement() % this.factories.size());
				}
				LOG.debug("Scheduling pre-starts for {}", factory.getFactoryName());
				factory.schedulePreStart();
			}
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to schedule PreStart", ex);
		}
	}
	
	@Override
	public void register(final PreStartableTCIFactory<?, ?> factory)
	{
		if(this.factoriesWeakSet.add(factory)) // Only add when not already present
		{
			this.factories.add(factory);
		}
	}
	
	@Override
	public void unregister(final PreStartableTCIFactory<?, ?> factory)
	{
		this.factories.remove(factory);
		this.factoriesWeakSet.remove(factory);
	}
	
	@Override
	public void close()
	{
		this.preStartScheduler.shutdown();
	}
}
