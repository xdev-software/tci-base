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
package software.xdev.tci.factory.registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.TCIFactory;


public class DefaultTCIFactoryRegistry implements TCIFactoryRegistry
{
	protected final Set<TCIFactory<?, ?>> factories = Collections.synchronizedSet(new HashSet<>());
	
	@Override
	public void register(final TCIFactory<?, ?> tciFactory)
	{
		this.factories.add(tciFactory);
	}
	
	@Override
	public void unRegister(final TCIFactory<?, ?> tciFactory)
	{
		this.factories.remove(tciFactory);
	}
	
	@Override
	public void warmUp()
	{
		final List<CompletableFuture<Void>> cfs = this.factories.stream()
			.map(f -> CompletableFuture.runAsync(f::warmUp))
			.toList();
		cfs.forEach(CompletableFuture::join);
	}
	
	@Override
	public Set<TCIFactory<?, ?>> getFactories()
	{
		return this.factories;
	}
	
	@Override
	@SuppressWarnings("java:S1452")
	public Map<TCIFactory<?, ?>, Set<TCI<?>>> getReturnedAndInUse()
	{
		return this.factories.stream()
			.filter(f -> !f.getReturnedAndInUse().isEmpty())
			.collect(Collectors.toMap(
				Function.identity(),
				f -> f.getReturnedAndInUse().stream()
					.map(x -> (TCI<?>)x)
					.collect(Collectors.toSet())));
	}
}
