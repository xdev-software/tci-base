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
package software.xdev.tci.serviceloading;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;


/**
 * Central point for service loading
 */
@SuppressWarnings("java:S6548")
public final class TCIServiceLoader
{
	private static final TCIServiceLoader INSTANCE = new TCIServiceLoader();
	
	public static TCIServiceLoader instance()
	{
		return INSTANCE;
	}
	
	private final Map<Class<?>, Object> loadedServices = Collections.synchronizedMap(new HashMap<>());
	
	private TCIServiceLoader()
	{
	}
	
	@SuppressWarnings("unchecked")
	public <T> T service(final Class<T> clazz)
	{
		return (T)this.loadedServices.computeIfAbsent(
			clazz,
			c -> ServiceLoader.load(clazz)
				.stream()
				// Get by highest priority
				.max(Comparator.comparingInt(p ->
					Optional.ofNullable(p.type().getAnnotation(TCIProviderPriority.class))
						.map(TCIProviderPriority::value)
						.orElse(TCIProviderPriority.DEFAULT_PRIORITY)))
				.map(ServiceLoader.Provider::get)
				.orElse(null));
	}
	
	public boolean isLoaded(final Class<?> clazz)
	{
		return this.loadedServices.get(clazz) != null;
	}
}
