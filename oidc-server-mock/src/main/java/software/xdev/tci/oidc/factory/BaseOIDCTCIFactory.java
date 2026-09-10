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
package software.xdev.tci.oidc.factory;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import software.xdev.tci.envperf.EnvironmentPerformance;
import software.xdev.tci.factory.prestart.PreStartableTCIFactory;
import software.xdev.tci.factory.prestart.config.PreStartConfig;
import software.xdev.tci.misc.ContainerMemory;
import software.xdev.tci.oidc.BaseOIDCTCI;
import software.xdev.tci.oidc.containers.BaseOIDCServerContainer;
import software.xdev.tci.oidc.containers.OIDCServerContainer;
import software.xdev.tci.startup.wait.FastAbortOnContainerDeathWaitStrategy;
import software.xdev.tci.startup.wait.strategy.HostPortWaitAbortableStrategy;
import software.xdev.tci.startup.wait.strategy.HttpWaitAbortableStrategy;


@SuppressWarnings("java:S119")
public abstract class BaseOIDCTCIFactory<
	SELF extends BaseOIDCTCIFactory<SELF, C, I>,
	C extends BaseOIDCServerContainer<C>,
	I extends BaseOIDCTCI<?, C, ?>>
	extends PreStartableTCIFactory<C, I>
{
	protected static final String DEFAULT_CONTAINER_LOGGER_NAME = "container.oidc";
	protected static final String DEFAULT_CONTAINER_BASE_NAME = "oidc";
	protected static final String DEFAULT_NAME = "OIDC";
	
	protected BaseOIDCTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder)
	{
		super(
			infraBuilder, containerBuilder, DEFAULT_CONTAINER_BASE_NAME, DEFAULT_CONTAINER_LOGGER_NAME,
			DEFAULT_NAME);
	}
	
	protected BaseOIDCTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final PreStartConfig config,
		final Timeouts timeouts)
	{
		super(
			infraBuilder, containerBuilder,
			DEFAULT_CONTAINER_BASE_NAME, DEFAULT_CONTAINER_LOGGER_NAME, DEFAULT_NAME, config, timeouts);
	}
	
	protected BaseOIDCTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName,
		final String name)
	{
		super(infraBuilder, containerBuilder, containerBaseName, containerLoggerName, name);
	}
	
	protected BaseOIDCTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName,
		final String name,
		final PreStartConfig config,
		final Timeouts timeouts)
	{
		super(infraBuilder, containerBuilder, containerBaseName, containerLoggerName, name, config, timeouts);
	}
	
	public static OIDCServerContainer createDefaultContainer()
	{
		return createDefaultContainer(null);
	}
	
	@SuppressWarnings({"checkstyle:MagicNumber"})
	public static OIDCServerContainer createDefaultContainer(final Consumer<OIDCServerContainer> customizer)
	{
		final OIDCServerContainer oidcServerContainer = new OIDCServerContainer()
			.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemory(ContainerMemory.M512M))
			.waitingFor(FastAbortOnContainerDeathWaitStrategy.waitAll(s -> s
				.withStartupTimeout(Duration.ofSeconds(40L + 20L * EnvironmentPerformance.cpuSlownessFactor()))
				.withStrategy(new HostPortWaitAbortableStrategy())
				.withStrategy(
					new HttpWaitAbortableStrategy()
						.forPort(BaseOIDCServerContainer.PORT)
						.forPath("/")
						.forStatusCode(200)
						.withReadTimeout(Duration.ofSeconds(10))
				))
			);
		
		if(customizer != null)
		{
			customizer.accept(oidcServerContainer);
		}
		
		return oidcServerContainer.withDefaultEnvConfig();
	}
	
	@SuppressWarnings("unchecked")
	public SELF self()
	{
		return (SELF)this;
	}
}
