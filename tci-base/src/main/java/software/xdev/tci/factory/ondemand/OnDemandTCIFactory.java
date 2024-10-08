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
package software.xdev.tci.factory.ondemand;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.BaseTCIFactory;


/**
 * A simple implementation of {@link software.xdev.tci.factory.TCIFactory}.
 * <p>
 * Creates new infrastructure on demand and allows customizing the container.
 * </p>
 * <p>
 * It's recommended to use this for certain infrastructure that is only required for a few tests
 * and that uses stateful container starts.
 * </p>
 */
public class OnDemandTCIFactory<C extends GenericContainer<C>, I extends TCI<C>>
	extends BaseTCIFactory<C, I>
{
	protected AtomicInteger startCounter = new AtomicInteger(1);
	
	public OnDemandTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName)
	{
		super(infraBuilder, containerBuilder, containerBaseName, containerLoggerName);
	}
	
	protected I newInternal(final Network network, final Consumer<C> buildContainerCustomizer)
	{
		final C c = this.buildContainer()
			.withNetwork(network);
		Optional.ofNullable(buildContainerCustomizer).ifPresent(customizer -> customizer.accept(c));
		
		final I infra = this.infraBuilder.apply(c, c.getNetworkAliases().stream()
			.skip(1) // At pos 0 is a generic one from GenericContainer
			.findFirst()
			.orElseGet(() -> c.getNetworkAliases().stream()
				.findFirst()
				.orElse(null)));
		try
		{
			infra.start(this.containerBaseName + "-" + this.startCounter.getAndIncrement());
		}
		catch(final RuntimeException rex)
		{
			this.handleInfraStartFail(infra);
			throw rex;
		}
		return infra;
	}
	
	public I getNew(final Network network, final Consumer<C> buildContainerCustomizer)
	{
		this.log().info("Getting new infra");
		final long startTime = System.currentTimeMillis();
		
		final I infra = this.registerReturned(Unreliables.retryUntilSuccess(
			this.getNewTryCount,
			() -> this.newInternal(network, buildContainerCustomizer)));
		
		final long ms = System.currentTimeMillis() - startTime;
		this.log().info("Got new infra, took {}ms", ms);
		
		this.tracer.timedAdd("getNew", ms);
		
		return infra;
	}
	
	public I getNew(final Network network)
	{
		return this.getNew(network, null);
	}
}
