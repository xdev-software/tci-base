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
package software.xdev.tci.factory.prestart.config;

import java.util.Optional;

import software.xdev.tci.config.DefaultConfig;


/**
 * Default implementation of {@link PreStartConfig} using {@link System#getProperties() System Properties}.
 * <p>
 * Properties can be defined in the following way:
 * <pre>
 * -Dtci.infra-pre-start.keep-ready=2
 * -Dtci.infra-pre-start.coordinator.idle-cpu-percent=50
 * </pre>
 * </p>
 */
public class DefaultPreStartConfig extends DefaultConfig implements PreStartConfig
{
	protected static final String KEEP_READY = "keep-ready";
	protected static final String MAX_START_SIMULTAN = "max-start-simultan";
	protected static final String DIRECT_NETWORK_ATTACH_IF_POSSIBLE = "direct-network-attach-if-possible";
	protected static final String FIXATE_EXPOSED_PORTS_IF_REQUIRED = "fixate-exposed-ports-if-required";
	protected static final String JUNIT_JUPITER_EXECUTION_PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE =
		"junit.jupiter.execution.parallel.config.fixed.max-pool-size";
	
	protected static final String COORDINATOR_IDLE_CPU_PERCENT = "coordinator.idle-cpu-percent";
	protected static final String COORDINATOR_SCHEDULE_PERIOD_MS = "coordinator.schedule-period-ms";
	
	protected static final String DETECT_ENDING_TESTS = "detect-ending-tests";
	
	protected final boolean enabled;
	
	protected final int keepReady;
	protected final int maxStartSimultan;
	protected final boolean directNetworkAttachIfPossible;
	protected final boolean fixateExposedPortsIfRequired;
	
	protected final int coordinatorIdleCPUPercent;
	protected final int coordinatorSchedulePeriodMs;
	
	protected final boolean detectEndingTests;
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public DefaultPreStartConfig()
	{
		this.enabled = this.resolveBool("enabled", DEFAULT_ENABLED);
		
		this.keepReady = this.enabled
			? this.resolveInt(
			KEEP_READY,
			() -> this.getSystemPropertyInt(JUNIT_JUPITER_EXECUTION_PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE, 1))
			: 0;
		this.maxStartSimultan = this.enabled
			? this.resolveInt(
			MAX_START_SIMULTAN,
			() -> this.getSystemPropertyInt(JUNIT_JUPITER_EXECUTION_PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE, 1))
			: -1;
		this.directNetworkAttachIfPossible =
			this.resolveBool(DIRECT_NETWORK_ATTACH_IF_POSSIBLE, DEFAULT_DIRECT_NETWORK_ATTACH_IF_POSSIBLE);
		this.fixateExposedPortsIfRequired =
			this.resolveBool(FIXATE_EXPOSED_PORTS_IF_REQUIRED, DEFAULT_FIXATE_EXPOSED_PORTS_IF_REQUIRED);
		
		this.coordinatorIdleCPUPercent = this.enabled
			? this.resolveInt(COORDINATOR_IDLE_CPU_PERCENT, DEFAULT_COORDINATOR_IDLE_CPU_PERCENT)
			: -1;
		this.coordinatorSchedulePeriodMs = this.enabled
			? this.resolveInt(COORDINATOR_SCHEDULE_PERIOD_MS, DEFAULT_COORDINATOR_SCHEDULE_PERIOD_MS)
			: -1;
		
		this.detectEndingTests = this.enabled
			&& this.resolveBool(DETECT_ENDING_TESTS, DEFAULT_DETECT_ENDING_TESTS);
	}
	
	@Override
	public String propertyNamePrefix()
	{
		return "tci.infra-pre-start";
	}
	
	@Override
	protected Optional<String> resolve(final String propertyName)
	{
		return super.resolve(propertyName)
			.or(() -> this.resolveFullyBuildPropertyName("infra-prestart." + propertyName)
				.map(v -> this.reportLegacyConfigOption(
					"infra-prestart." + propertyName,
					this.propertyNamePrefix() + "." + propertyName,
					v)
				)
			);
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
			? this.resolveInt(preStartName + "." + KEEP_READY, this.keepReady)
			: this.keepReady);
	}
	
	@Override
	public int maxStartSimultan(final String preStartName)
	{
		return this.enabled()
			? this.resolveInt(preStartName + "." + MAX_START_SIMULTAN, this.maxStartSimultan)
			: this.maxStartSimultan;
	}
	
	@Override
	public boolean directNetworkAttachIfPossible(final String preStartName)
	{
		return this.enabled()
			? this.resolveBool(
			preStartName + "." + DIRECT_NETWORK_ATTACH_IF_POSSIBLE,
			this.directNetworkAttachIfPossible)
			: this.directNetworkAttachIfPossible;
	}
	
	@Override
	public boolean fixateExposedPortsIfRequired(final String preStartName)
	{
		return this.enabled()
			? this.resolveBool(
			preStartName + "." + FIXATE_EXPOSED_PORTS_IF_REQUIRED,
			this.fixateExposedPortsIfRequired)
			: this.fixateExposedPortsIfRequired;
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
	
	protected int getSystemPropertyInt(final String property, final int defaultVal)
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
			.orElse(defaultVal);
	}
}
