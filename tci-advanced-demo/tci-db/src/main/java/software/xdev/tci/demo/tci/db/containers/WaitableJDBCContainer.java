package software.xdev.tci.demo.tci.db.containers;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;


interface WaitableJDBCContainer extends WaitStrategyTarget
{
	default WaitStrategy completeJDBCWaitStrategy()
	{
		return new WaitAllStrategy()
			.withStrategy(Wait.defaultWaitStrategy())
			.withStrategy(new JDBCWaitStrategy());
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
	
	String getTestQueryString();
	
	/**
	 * @apiNote Assumes that the container is already started
	 */
	class JDBCWaitStrategy extends AbstractWaitStrategy
	{
		@SuppressWarnings("checkstyle:MagicNumber")
		public JDBCWaitStrategy()
		{
			this.withRateLimiter(RateLimiterBuilder.newBuilder()
				.withRate(200, TimeUnit.MILLISECONDS)
				.withConstantThroughput()
				.build());
		}
		
		@SuppressWarnings("PMD.PreserveStackTrace")
		@Override
		protected void waitUntilReady()
		{
			if(!(this.waitStrategyTarget instanceof final JdbcDatabaseContainer<?> container
				&& this.waitStrategyTarget instanceof final WaitableJDBCContainer waitableJDBCContainer))
			{
				throw new IllegalArgumentException(
					"Container must implement JdbcDatabaseContainer and WaitableJDBCContainer");
			}
			
			try
			{
				Unreliables.retryUntilTrue(
					(int)this.startupTimeout.getSeconds(),
					TimeUnit.SECONDS,
					() -> this.getRateLimiter().getWhenReady(() -> {
						try(final Connection connection = container.createConnection("");
							final Statement statement = connection.createStatement())
						{
							return statement.execute(waitableJDBCContainer.getTestQueryString());
						}
					})
				);
			}
			catch(final TimeoutException e)
			{
				throw new ContainerLaunchException(
					"JDBCContainer cannot be accessed by (JDBC URL: "
						+ container.getJdbcUrl()
						+ "), please check container logs");
			}
		}
	}
}
