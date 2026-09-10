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
package software.xdev.tci.mockserver.factory;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.testcontainers.containers.Network;

import software.xdev.tci.factory.ondemand.OnDemandTCIFactory;
import software.xdev.tci.mockserver.MockServerTCI;
import software.xdev.tci.mockserver.containers.TCIMockserverContainer;
import software.xdev.testcontainers.mockserver.containers.MockServerContainer;


public abstract class MockServerTCIFactory<I extends MockServerTCI>
	extends OnDemandTCIFactory<MockServerContainer, I>
{
	protected MockServerTCIFactory(
		final BiFunction<MockServerContainer, String, I> infraBuilder,
		final Supplier<MockServerContainer> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName)
	{
		super(infraBuilder, containerBuilder, containerBaseName, containerLoggerName);
	}
	
	protected MockServerTCIFactory(
		final BiFunction<MockServerContainer, String, I> infraBuilder,
		final String additionalContainerBaseName,
		final String additionalLoggerName)
	{
		super(
			infraBuilder,
			TCIMockserverContainer::createDefaultForFactory,
			"mockserver-" + additionalContainerBaseName,
			"container.mockserver." + additionalLoggerName);
	}
	
	public I getNew(
		final Network network,
		final String name,
		final String... networkAliases)
	{
		return this.getNew(
			network, c -> c.withNetworkAliases(networkAliases)
				.setLogConsumers(List.of(getLogConsumer(this.containerLoggerName + "." + name))));
	}
	
	@Override
	public I getNew(final Network network)
	{
		throw new UnsupportedOperationException();
	}
}
