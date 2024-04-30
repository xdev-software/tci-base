package software.xdev.tci.factory.prestart.config;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;


/**
 * Default implementation of {@link PreStartConfig} using system properties.
 */
public class DefaultPreStartConfig implements PreStartConfig
{
	protected static final String PROPERTY_PREFIX = "infra-pre-start.";
	
	protected static final String KEEP_READY = "keep-ready";
	protected static final String MAX_START_SIMULTAN = "max-start-simultan";
	protected static final String DIRECT_NETWORK_ATTACH_IF_POSSIBLE = "direct-network-attach-if-possible";
	protected static final String JUNIT_JUPITER_EXECUTION_PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE =
		"junit.jupiter.execution.parallel.config.fixed.max-pool-size";
	
	protected static final String COORDINATOR_IDLE_CPU_PERCENT = "coordinator.idle-cpu-percent";
	protected static final String COORDINATOR_SCHEDULE_PERIOD_MS = "coordinator.schedule-period-ms";
	
	protected static final String DETECT_ENDING_TESTS = "detect-ending-tests";
	
	protected final boolean enabled;
	
	protected final int keepReady;
	protected final int maxStartSimultan;
	protected final boolean directNetworkAttachIfPossible;
	
	protected final int coordinatorIdleCPUPercent;
	protected final int coordinatorSchedulePeriodMs;
	
	protected final boolean detectEndingTests;
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public DefaultPreStartConfig()
	{
		this.enabled = this.getBool(PROPERTY_PREFIX + "enabled", false);
		
		this.keepReady = this.enabled
			? this.getInt(
			PROPERTY_PREFIX + KEEP_READY,
			() -> this.getInt(JUNIT_JUPITER_EXECUTION_PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE, 1))
			: 0;
		this.maxStartSimultan = this.enabled
			? this.getInt(
			PROPERTY_PREFIX + MAX_START_SIMULTAN,
			() -> this.getInt(JUNIT_JUPITER_EXECUTION_PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE, 1))
			: -1;
		this.directNetworkAttachIfPossible =
			this.getBool(PROPERTY_PREFIX + DIRECT_NETWORK_ATTACH_IF_POSSIBLE, true);
		
		this.coordinatorIdleCPUPercent = this.enabled
			? this.getInt(PROPERTY_PREFIX + COORDINATOR_IDLE_CPU_PERCENT, 40)
			: -1;
		this.coordinatorSchedulePeriodMs = this.enabled
			? this.getInt(PROPERTY_PREFIX + COORDINATOR_SCHEDULE_PERIOD_MS, 1_000)
			: -1;
		
		this.detectEndingTests = this.enabled
			&& this.getBool(PROPERTY_PREFIX + DETECT_ENDING_TESTS, true);
	}
	
	@Override
	public boolean enabled()
	{
		return this.enabled;
	}
	
	@Override
	public int keepReady(final String preStartName)
	{
		return Math.max(0, this.enabled()
			? this.getInt(PROPERTY_PREFIX + preStartName + "." + KEEP_READY, this.keepReady)
			: this.keepReady);
	}
	
	@Override
	public int maxStartSimultan(final String preStartName)
	{
		return this.enabled()
			? this.getInt(PROPERTY_PREFIX + preStartName + "." + MAX_START_SIMULTAN, this.maxStartSimultan)
			: this.maxStartSimultan;
	}
	
	@Override
	public boolean directNetworkAttachIfPossible(final String preStartName)
	{
		return this.enabled()
			?
			this.getBool(
				PROPERTY_PREFIX + preStartName + "." + DIRECT_NETWORK_ATTACH_IF_POSSIBLE,
				this.directNetworkAttachIfPossible)
			: this.directNetworkAttachIfPossible;
	}
	
	@Override
	public int coordinatorIdleCPUPercent()
	{
		return this.coordinatorIdleCPUPercent;
	}
	
	@Override
	public int coordinatorSchedulePeriodMs()
	{
		return this.coordinatorSchedulePeriodMs;
	}
	
	@Override
	public boolean detectEndingTests()
	{
		return this.detectEndingTests;
	}
	
	// region Get
	protected boolean getBool(final String property, final boolean defaultValue)
	{
		return this.getBool(property, () -> defaultValue);
	}
	
	protected boolean getBool(
		final String property,
		final BooleanSupplier defaultValueSupplier)
	{
		return Optional.ofNullable(System.getProperty(property))
			.map(v -> "1".equals(v) || Boolean.parseBoolean(v))
			.orElseGet(defaultValueSupplier::getAsBoolean);
	}
	
	protected int getInt(final String property, final int defaultValue)
	{
		return this.getInt(property, () -> defaultValue);
	}
	
	protected int getInt(final String property, final IntSupplier defaultValueSupplier)
	{
		return Optional.ofNullable(System.getProperty(property))
			.map(s -> {
				try
				{
					return Integer.parseInt(s);
				}
				catch(final NumberFormatException nfe)
				{
					return null;
				}
			})
			.orElseGet(defaultValueSupplier::getAsInt);
	}
	// endregion
}
