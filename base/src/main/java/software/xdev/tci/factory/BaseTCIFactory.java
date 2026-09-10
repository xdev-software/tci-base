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
package software.xdev.tci.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import software.xdev.tci.TCI;
import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.tracing.TCITracer;


@SuppressWarnings("java:S119")
public abstract class BaseTCIFactory<
	C extends GenericContainer<C>,
	I extends TCI<C>>
	implements TCIFactory<C, I>
{
	private final Logger logger;
	/**
	 * Describes infra that is currently in use.
	 * <p>
	 * The future is completed when the infra is stopped.
	 * </p>
	 */
	protected Map<I, CompletableFuture<Boolean>> returnedAndInUse = new ConcurrentHashMap<>();
	protected boolean warmedUp;
	/**
	 * Describes how often new infra should be created/started - if it fails.
	 * <p>
	 * This helps with "Random" errors that occur during infra startup. For example when a port allocation fails.
	 * </p>
	 */
	protected int getNewAttempts = 2;
	
	protected BiFunction<C, String, I> infraBuilder;
	protected final Supplier<C> containerBuilder;
	protected final String containerBaseName;
	protected final String containerLoggerName;
	protected final TCITracer tracer = new TCITracer();
	
	protected BaseTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName)
	{
		this.infraBuilder = Objects.requireNonNull(infraBuilder);
		
		this.containerBuilder = Objects.requireNonNull(containerBuilder);
		this.containerBaseName = Objects.requireNonNull(containerBaseName);
		this.containerLoggerName = Objects.requireNonNull(containerLoggerName);
		
		this.logger = LoggerFactory.getLogger(this.getClass());
		
		this.register();
	}
	
	@Override
	public void warmUp()
	{
		if(!this.warmedUp)
		{
			this.warmUpSync();
		}
	}
	
	protected synchronized void warmUpSync()
	{
		if(this.warmedUp)
		{
			return;
		}
		
		final long startTime = System.currentTimeMillis();
		
		this.warmUpInternal();
		this.warmedUp = true;
		
		this.tracer.timedAdd("warmUp", System.currentTimeMillis() - startTime);
	}
	
	protected void warmUpInternal()
	{
		// No OP
	}
	
	protected C buildContainer()
	{
		return this.containerBuilder.get()
			.withLogConsumer(getLogConsumer(this.containerLoggerName));
	}
	
	protected void handleInfraStartFail(final I infra)
	{
		CompletableFuture.runAsync(() -> {
			final long startTime = System.currentTimeMillis();
			try
			{
				infra.stop();
			}
			catch(final Exception stopEx)
			{
				this.log().warn("Failed to cleanup infra that failed during startup", stopEx);
			}
			this.tracer.timedAdd("infraStartFailCleanup", System.currentTimeMillis() - startTime);
		}, TCIExecutorServiceHolder.instance());
	}
	
	protected I getNewWithRetryAndRegisterReturned(final Supplier<I> supplier)
	{
		return this.registerReturned(this.getNewWithRetry(supplier));
	}
	
	protected I getNewWithRetry(final Supplier<I> supplier)
	{
		final List<RuntimeException> failedTries = new ArrayList<>();
		for(int attempt = 1; attempt <= this.getNewAttempts; attempt++)
		{
			try
			{
				return supplier.get();
			}
			catch(final RuntimeException ex)
			{
				if(attempt == this.getNewAttempts)
				{
					failedTries.forEach(ex::addSuppressed);
					this.logger.error("All {}x attempts getting new infra failed", attempt);
					throw ex;
				}
				this.logger.warn(
					"Unexpected error while getting new infra on attempt {}/{}. "
						+ "If you encounter this regularly please check "
						+ "the configured timeouts and container resource limitations. "
						+ "Retrying now",
					attempt,
					this.getNewAttempts,
					ex);
				failedTries.add(ex);
			}
		}
		throw new IllegalStateException("attempts (" + this.getNewAttempts + ") is invalid");
	}
	
	protected I registerReturned(final I infra)
	{
		this.returnedAndInUse.put(infra, new CompletableFuture<>());
		infra.setOnStopped(() ->
			// Remove it from the "in use" list
			this.returnedAndInUse.remove(infra)
				// Mark its stop future as completed
				.complete(true));
		
		return infra;
	}
	
	@Override
	public void close()
	{
		// NO OP
	}
	
	@Override
	public Map<I, CompletableFuture<Boolean>> getReturnedAndInUse()
	{
		return new HashMap<>(this.returnedAndInUse);
	}
	
	@Override
	public TCITracer getTracer()
	{
		return this.tracer;
	}
	
	public void setGetNewAttempts(final int attempts)
	{
		if(attempts <= 0)
		{
			throw new IllegalArgumentException("must be greater than 0");
		}
		this.getNewAttempts = attempts;
	}
	
	protected Logger log()
	{
		return this.logger;
	}
	
	protected static Slf4jLogConsumer getLogConsumer(final String name)
	{
		return new Slf4jLogConsumer(LoggerFactory.getLogger(name));
	}
}
