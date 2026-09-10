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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import software.xdev.tci.startup.wait.AbortMonitor;
import software.xdev.tci.startup.wait.holder.AbortableStrategyValuesHolder;


public abstract class AbstractWaitAbortableStrategy<S extends AbstractWaitAbortableStrategy<S>>
	extends AbstractWaitStrategy
{
	protected <T> T startupRetryUntilSuccess(final Callable<T> callable)
	{
		return Unreliables.retryUntilSuccess(
			(int)this.startupTimeout.getSeconds(),
			TimeUnit.SECONDS,
			callable
		);
	}
	
	protected void startupRetryUntilSuccessWithRateLimitWhenNotAborted(
		final AbortMonitor abortMonitor,
		final Runnable runnable)
	{
		this.startupRetryUntilSuccess(() -> {
			if(!abortMonitor.shouldAbort())
			{
				this.getRateLimiter().doWhenReady(runnable);
			}
			return true;
		});
	}
	
	@Override
	protected void waitUntilReady()
	{
		this.waitUntilReady(AbortableStrategyValuesHolder.get().abortMonitor());
	}
	
	protected void waitUntilReady(final AbortMonitor abortMonitor)
	{
		// NOOP
	}
	
	protected ExecutorService getExecutor()
	{
		return AbortableStrategyValuesHolder.get().executor();
	}
	
	@SuppressWarnings("unchecked")
	protected S self()
	{
		return (S)this;
	}
}
