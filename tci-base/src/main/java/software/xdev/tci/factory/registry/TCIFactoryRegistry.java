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

import java.util.Map;
import java.util.Set;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.TCIFactory;
import software.xdev.tci.serviceloading.TCIServiceLoader;


/**
 * Registry for all factories that create infrastructure.
 * <p>
 * e.g. used by various agents to enumerate all factories
 * </p>
 */
@SuppressWarnings("java:S1452")
public interface TCIFactoryRegistry
{
	void register(final TCIFactory<?, ?> tciFactory);
	
	void unRegister(final TCIFactory<?, ?> tciFactory);
	
	void warmUp();
	
	Set<TCIFactory<?, ?>> getFactories();
	
	Map<TCIFactory<?, ?>, Set<TCI<?>>> getReturnedAndInUse();
	
	static TCIFactoryRegistry instance()
	{
		return TCIServiceLoader.instance().service(TCIFactoryRegistry.class);
	}
}
