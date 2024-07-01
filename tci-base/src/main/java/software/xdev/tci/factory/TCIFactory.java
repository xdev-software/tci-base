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
package software.xdev.tci.factory;

import java.util.Set;

import org.testcontainers.containers.GenericContainer;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.tracing.TCITracer;


/**
 * A factory for {@link TCI}
 */
public interface TCIFactory<C extends GenericContainer<C>, I extends TCI<C>> extends AutoCloseable
{
	default void register()
	{
		TCIFactoryRegistry.instance().register(this);
	}
	
	default void unregister()
	{
		TCIFactoryRegistry.instance().unRegister(this);
	}
	
	void warmUp();
	
	@Override
	void close();
	
	Set<I> getReturnedAndInUse();
	
	default String getFactoryName()
	{
		return this.getClass().getSimpleName();
	}
	
	TCITracer getTracer();
}
