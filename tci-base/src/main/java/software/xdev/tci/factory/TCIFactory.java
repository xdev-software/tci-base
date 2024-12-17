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
	
	/**
	 * Does some warm up work, e.g. building images so that they don't have to be pulled later.
	 *
	 * @apiNote There is no guarantee that this method will be called (or how often).
	 * <p>
	 * It's e.g. possible that this method will not be called if a factory was created after the initial warm up phase.
	 * </p>
	 */
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
