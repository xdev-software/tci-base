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

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Central point for service loading
 */
@SuppressWarnings({"java:S6548", "java:S2789"}) // Don't force us to write our own Optional!?!
public class TCIServiceLoader
{
	private static final Logger LOG = LoggerFactory.getLogger(TCIServiceLoader.class);
	
	protected final Object globalInitServiceLockHandlerLock = new Object();
	protected final InheritableThreadLocal<LinkedHashSet<Class<?>>> tlDetectRecursiveInitServices =
		new InheritableThreadLocal<>();
	protected final Map<Class<?>, ReentrantLock> servicesLoadingSyncLocks = new ConcurrentHashMap<>();
	protected final Map<Class<?>, Optional<?>> loadedServices = new ConcurrentHashMap<>();
	
	public <T> T service(final Class<T> clazz)
	{
		// Do not use computeIfAbsent here. This might cause "recursive updates" exceptions.
		// These are usually not recursive, there is just a call here doing another call with a different type
		// Recursive updates will be detected by initService
		
		if(!this.loadedServices.containsKey(clazz))
		{
			this.initService(clazz);
		}
		
		return this.loadedServices.get(clazz)
			.map(clazz::cast)
			.orElse(null);
	}
	
	/**
	 * @implNote This method uses best effort thread safety. It WILL fail when another Thread that didn't inherit from
	 * the current Thread is causing a deadlock, for example <code>ForkJoinPool.commonPool</code>
	 */
	@SuppressWarnings("PMD.AvoidSynchronizedStatement")
	protected <T> void initService(final Class<T> clazz)
	{
		final LinkedHashSet<Class<?>> detectRecursiveInitServices;
		final ReentrantLock lock;
		synchronized(this.globalInitServiceLockHandlerLock)
		{
			detectRecursiveInitServices = Optional.ofNullable(this.tlDetectRecursiveInitServices.get())
				.map(LinkedHashSet::new)
				.orElseGet(LinkedHashSet::new);
			
			if(detectRecursiveInitServices.contains(clazz))
			{
				throw new IllegalStateException("Detected recursive initialization on "
					+ clazz
					+ "; chain trying class creation: "
					+ detectRecursiveInitServices.stream()
					.map(Class::getName)
					.collect(Collectors.joining(" -> ")));
			}
			
			detectRecursiveInitServices.add(clazz);
			this.tlDetectRecursiveInitServices.set(detectRecursiveInitServices);
			
			lock = this.servicesLoadingSyncLocks.computeIfAbsent(
				clazz,
				ignored -> new ReentrantLock());
			this.reportLockAction("got", clazz);
		}
		
		lock.lock();
		this.reportLockAction("acquired", clazz);
		
		try
		{
			// Already initialized?
			if(this.loadedServices.containsKey(clazz))
			{
				return;
			}
			
			this.loadedServices.put(clazz, this.loadService(clazz));
		}
		catch(final RuntimeException rex)
		{
			LOG.debug("Service loading failed", rex);
			throw rex;
		}
		finally
		{
			synchronized(this.globalInitServiceLockHandlerLock)
			{
				detectRecursiveInitServices.remove(clazz);
				this.tlDetectRecursiveInitServices.set(detectRecursiveInitServices);
				
				this.servicesLoadingSyncLocks.remove(clazz);
				lock.unlock();
				this.reportLockAction("release", clazz);
			}
		}
	}
	
	protected <T> void reportLockAction(final String action, final Class<T> clazz)
	{
		if(LOG.isTraceEnabled())
		{
			LOG.trace(
				"Lock {} {} - T:{}",
				action,
				clazz,
				Thread.currentThread().getName(),
				new RuntimeException());
		}
	}
	
	protected <T> Optional<T> loadService(final Class<T> clazz)
	{
		return ServiceLoader.load(clazz)
			.stream()
			// Get by highest priority
			.max(Comparator.comparingInt(p ->
				Optional.ofNullable(p.type().getAnnotation(TCIProviderPriority.class))
					.map(TCIProviderPriority::value)
					.orElse(TCIProviderPriority.DEFAULT_PRIORITY)))
			.map(ServiceLoader.Provider::get);
	}
	
	public boolean isLoaded(final Class<?> clazz)
	{
		final Optional<?> optImpl = this.loadedServices.get(clazz);
		return optImpl != null && optImpl.isPresent();
	}
	
	/**
	 * Force overwrite the loaded configuration manually.
	 * <p>
	 * WARNING: Usage not recommended. Should only be used as last resort!
	 * </p>
	 */
	public void forceOverwrite(final Class<?> clazz, final Object value)
	{
		this.loadedServices.put(clazz, Optional.ofNullable(value));
	}
}
