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
package software.xdev.tci.selenium.factory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testcontainers.containers.Network;

import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.factory.TCIFactory;
import software.xdev.tci.selenium.BrowserTCI;
import software.xdev.tci.selenium.CapabilityFactory;
import software.xdev.tci.selenium.TestBrowser;
import software.xdev.tci.selenium.containers.SeleniumBrowserWebDriverContainer;
import software.xdev.tci.tracing.TCITracer;


/**
 * Contains multiple {@link BrowserTCIFactory}s - usually one per browser/capabilities - and manages them.
 */
public class BrowsersTCIFactory<F extends CapabilityFactory>
	implements TCIFactory<SeleniumBrowserWebDriverContainer, BrowserTCI>
{
	protected final Map<CapabilityFactory, BrowserTCIFactory> browserFactories = new ConcurrentHashMap<>();
	
	public static BrowsersTCIFactory<TestBrowser> createDefault()
	{
		return new BrowsersTCIFactory<>(TestBrowser.values());
	}
	
	@SafeVarargs
	public BrowsersTCIFactory(final F... caps)
	{
		this(Arrays.stream(caps));
	}
	
	public BrowsersTCIFactory(final Stream<F> caps)
	{
		caps.forEach(factory ->
			this.browserFactories.put(factory, new BrowserTCIFactory(factory.createCapabilities())));
	}
	
	public BrowsersTCIFactory(final Map<F, BrowserTCIFactory> browserFactories)
	{
		this.browserFactories.putAll(browserFactories);
	}
	
	public BrowsersTCIFactory<F> withPullVideoRecordingContainerOnWarmUp(final boolean pull)
	{
		this.browserFactories.values().forEach(f -> f.withPullVideoRecordingContainerOnWarmUp(pull));
		return this;
	}
	
	@Override
	public void warmUp()
	{
		// No effect - Done in downstream factories
	}
	
	public BrowserTCI getNew(
		final F factory,
		final Network network,
		final String... networkAliases)
	{
		final BrowserTCIFactory infraFactory = this.browserFactories.get(factory);
		if(infraFactory == null)
		{
			throw new IllegalStateException(
				"Requested factory " + factory + " was not found/registered during initialization! "
					+ "Make sure to register it and ensure that equals/hashCode are correctly implemented");
		}
		return infraFactory.getNew(network, networkAliases);
	}
	
	@Override
	public void close()
	{
		final List<CompletableFuture<Void>> cfFactories = this.browserFactories.values().stream()
			.map(f -> CompletableFuture.runAsync(f::close, TCIExecutorServiceHolder.instance()))
			.toList();
		cfFactories.forEach(CompletableFuture::join);
	}
	
	@Override
	public Map<BrowserTCI, CompletableFuture<Boolean>> getReturnedAndInUse()
	{
		return this.browserFactories.values()
			.stream()
			.map(BrowserTCIFactory::getReturnedAndInUse)
			.map(Map::entrySet)
			.flatMap(Collection::stream)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
	
	@Override
	public TCITracer getTracer()
	{
		return null;
	}
}
