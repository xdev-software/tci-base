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
package software.xdev.tci.startup.wait.strategy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.timeouts.Timeouts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import software.xdev.tci.startup.wait.AbortMonitor;
import software.xdev.tci.startup.wait.holder.AbortableStrategyValues;
import software.xdev.tci.startup.wait.holder.AbortableStrategyValuesHolder;


/**
 * Based on {@link WaitAllStrategy}
 */
public class WaitAllAbortableStrategy extends AbstractWaitAbortableStrategy<WaitAllAbortableStrategy>
{
	private static final Logger LOG = LoggerFactory.getLogger(WaitAllAbortableStrategy.class);
	
	protected final WaitAllStrategy.Mode mode;
	
	protected final List<WaitStrategy> strategies = new ArrayList<>();
	
	public WaitAllAbortableStrategy()
	{
		this(WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT);
	}
	
	public WaitAllAbortableStrategy(final WaitAllStrategy.Mode mode)
	{
		this.mode = mode;
	}
	
	@Override
	protected void waitUntilReady(final AbortMonitor abortMonitor)
	{
		if(this.mode == WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY)
		{
			this.waitUntilNestedStrategiesAreReady(abortMonitor);
		}
		else
		{
			final AbortableStrategyValues valuesToPropagate = AbortableStrategyValuesHolder.get();
			Timeouts.doWithTimeout(
				(int)this.startupTimeout.toMillis(),
				TimeUnit.MILLISECONDS,
				() -> AbortableStrategyValuesHolder.executeWith(
					() -> this.waitUntilNestedStrategiesAreReady(abortMonitor),
					valuesToPropagate)
			);
		}
	}
	
	private void waitUntilNestedStrategiesAreReady(final AbortMonitor abortMonitor)
	{
		for(final WaitStrategy strategy : this.strategies)
		{
			abortMonitor.throwIfRequired();
			
			final long startMs = System.currentTimeMillis();
			LOG.debug("Waiting for {}", strategy);
			
			strategy.waitUntilReady(this.waitStrategyTarget);
			
			LOG.debug("Finished waiting for {}, took {}ms", strategy, System.currentTimeMillis() - startMs);
		}
	}
	
	public WaitAllAbortableStrategy withStrategy(final AbstractWaitAbortableStrategy<?> strategy)
	{
		return this.withWaitStrategy(strategy);
	}
	
	/**
	 * @apiNote It's recommended to use {@link #withStrategy(AbstractWaitAbortableStrategy)}
	 */
	public WaitAllAbortableStrategy withWaitStrategy(final WaitStrategy strategy)
	{
		if(this.mode == WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT)
		{
			this.applyStartupTimeout(strategy);
		}
		
		this.strategies.add(strategy);
		return this.self();
	}
	
	@Override
	public WaitAllAbortableStrategy withStartupTimeout(final Duration startupTimeout)
	{
		if(this.mode == WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY)
		{
			throw new IllegalStateException(
				String.format(
					"Changing startup timeout is not supported with mode %s",
					WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY
				)
			);
		}
		
		super.withStartupTimeout(startupTimeout);
		
		this.strategies.forEach(this::applyStartupTimeout);
		return this.self();
	}
	
	private void applyStartupTimeout(final WaitStrategy childStrategy)
	{
		childStrategy.withStartupTimeout(this.startupTimeout);
	}
}
