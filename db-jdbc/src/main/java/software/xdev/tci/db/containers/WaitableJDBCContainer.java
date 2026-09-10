/*
 * Copyright © 2025 XDEV Software (https://xdev.software)
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
package software.xdev.tci.db.containers;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import software.xdev.tci.envperf.EnvironmentPerformance;
import software.xdev.tci.startup.wait.AbortMonitor;
import software.xdev.tci.startup.wait.FastAbortOnContainerDeathWaitStrategy;
import software.xdev.tci.startup.wait.strategy.AbstractWaitAbortableStrategy;
import software.xdev.tci.startup.wait.strategy.HostPortWaitAbortableStrategy;


public interface WaitableJDBCContainer extends WaitStrategyTarget
{
	@SuppressWarnings("checkstyle:MagicNumber")
	default WaitStrategy completeJDBCWaitStrategy()
	{
		return FastAbortOnContainerDeathWaitStrategy.waitAll(s -> s
			.withStartupTimeout(Duration.ofSeconds(40L + EnvironmentPerformance.cpuSlownessFactor() * 20L))
			.withStrategy(new HostPortWaitAbortableStrategy())
			.withStrategy(new JDBCWaitStrategy())
		);
	}
	
	WaitStrategy getWaitStrategy();
	
	default void waitUntilContainerStarted()
	{
		final WaitStrategy waitStrategy = this.getWaitStrategy();
		if(waitStrategy != null)
		{
			waitStrategy.waitUntilReady(this);
		}
	}
	
	/**
	 * @apiNote Assumes that the container is already started
	 */
	class JDBCWaitStrategy extends AbstractWaitAbortableStrategy<JDBCWaitStrategy>
	{
		@SuppressWarnings("checkstyle:MagicNumber")
		public JDBCWaitStrategy()
		{
			this.withRateLimiter(RateLimiterBuilder.newBuilder()
				.withRate(5, TimeUnit.SECONDS)
				.withConstantThroughput()
				.build());
		}
		
		@Override
		protected void waitUntilReady(final AbortMonitor abortMonitor)
		{
			if(!(this.waitStrategyTarget instanceof final JdbcDatabaseContainer<?> container))
			{
				throw new IllegalArgumentException(
					"Container must implement JdbcDatabaseContainer and WaitableJDBCContainer");
			}
			
			// Rate limit creation of connections as this is quite an expensive operation
			this.startupRetryUntilSuccessWithRateLimitWhenNotAborted(
				abortMonitor,
				() -> {
					try(final Connection connection = container.createConnection(""))
					{
						this.waitUntilJDBCConnectionValidated(container, connection);
					}
					catch(final SQLException sqlEx)
					{
						throw new IllegalStateException("SQL failed", sqlEx);
					}
				}
			);
		}
		
		@SuppressWarnings({"unused", "checkstyle:MagicNumber"}) // Parameter might be used by extension
		protected void waitUntilJDBCConnectionValidated(
			final JdbcDatabaseContainer<?> container,
			final Connection connection)
		{
			final RateLimiter validateJDBCRateLimiter = RateLimiterBuilder.newBuilder()
				.withRate(20, TimeUnit.SECONDS)
				.withConstantThroughput()
				.build();
			
			Unreliables.retryUntilSuccess(
				// If this fails after startupTimeout / 2 (min=3s, max=30s)
				// it might be possible that the connection is somehow corrupted or broken
				// -> Build a new connection after that (see waitUntilJDBCValid)
				Math.min(Math.max((int)this.startupTimeout.getSeconds() / 2, 3), 30),
				TimeUnit.SECONDS,
				() -> validateJDBCRateLimiter.getWhenReady(() -> this.validateJDBCConnection(connection)));
		}
		
		@SuppressWarnings("java:S112")
		protected boolean validateJDBCConnection(final Connection connection) throws Exception
		{
			return connection.isValid(10);
		}
	}
}
