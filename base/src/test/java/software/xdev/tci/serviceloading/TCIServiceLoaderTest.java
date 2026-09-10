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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.rnorth.ducttape.timeouts.Timeouts;


class TCIServiceLoaderTest
{
	@Test
	void normal()
	{
		final MockTCIServiceLoader loader = this.setupLoader(Map.of(
			RecursiveServiceA.class, ref -> () -> new RecursiveServiceA(ref.get(), false),
			RecursiveServiceB.class, ref -> RecursiveServiceB::new
		));
		assertDoesNotThrow(() -> loader.service(RecursiveServiceA.class));
	}
	
	@Test
	void recursive()
	{
		final MockTCIServiceLoader loader = this.setupLoader(Map.of(
			RecursiveServiceA.class, ref -> () -> new RecursiveServiceA(ref.get(), false),
			RecursiveServiceB.class, ref -> () -> new RecursiveServiceB(ref.get(), false)
		));
		assertThrows(IllegalStateException.class, () -> loader.service(RecursiveServiceA.class));
	}
	
	@Test
	void recursiveAsync()
	{
		this.runAsyncAndProvokeRecursiveDetectionBypass(() -> {
			final MockTCIServiceLoader loader = this.setupLoader(Map.of(
				RecursiveServiceA.class, ref -> () -> new RecursiveServiceA(ref.get(), true),
				RecursiveServiceB.class, ref -> () -> new RecursiveServiceB(ref.get(), true)
			));
			return () -> loader.service(RecursiveServiceA.class);
		});
	}
	
	@Test
	void recursiveSelf()
	{
		final MockTCIServiceLoader loader = this.setupLoader(Map.of(
			RecursiveServiceSelf.class, ref -> () -> new RecursiveServiceSelf(ref.get(), false)
		));
		assertThrows(IllegalStateException.class, () -> loader.service(RecursiveServiceSelf.class));
	}
	
	@Test
	void recursiveSelfAsync()
	{
		this.runAsyncAndProvokeRecursiveDetectionBypass(() -> {
			final MockTCIServiceLoader loader = this.setupLoader(Map.of(
				RecursiveServiceSelf.class, ref -> () -> new RecursiveServiceSelf(ref.get(), true)
			));
			return () -> loader.service(RecursiveServiceSelf.class);
		});
	}
	
	private void runAsyncAndProvokeRecursiveDetectionBypass(final Supplier<Runnable> supplyRun)
	{
		for(int i = 0; i < 20; i++) // try to provoke race condition
		{
			final Runnable run = supplyRun.get();
			final RuntimeException ex = assertThrows(
				RuntimeException.class,
				() -> Timeouts.doWithTimeout(3, TimeUnit.SECONDS, run));
			final CompletionException ex2 = assertInstanceOf(CompletionException.class, ex.getCause());
			assertInstanceOf(IllegalStateException.class, ex2.getCause());
		}
	}
	
	private MockTCIServiceLoader setupLoader(
		final Map<Class<?>, Function<AtomicReference<MockTCIServiceLoader>, Supplier<Object>>> predefinedImpls)
	{
		final AtomicReference<MockTCIServiceLoader> ref = new AtomicReference<>();
		final MockTCIServiceLoader loader = new MockTCIServiceLoader(predefinedImpls.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().apply(ref))));
		ref.set(loader);
		return loader;
	}
	
	static class MockTCIServiceLoader extends TCIServiceLoader
	{
		private final Map<Class<?>, Supplier<Object>> predefinedImpls;
		
		protected MockTCIServiceLoader(final Map<Class<?>, Supplier<Object>> predefinedImpls)
		{
			this.predefinedImpls = predefinedImpls;
		}
		
		@Override
		protected <T> Optional<T> loadService(final Class<T> clazz)
		{
			return Optional.ofNullable(this.predefinedImpls.get(clazz))
				.map(Supplier::get)
				.map(clazz::cast);
		}
	}
	
	
	abstract static class BaseRecursiveService
	{
		protected BaseRecursiveService()
		{
		}
		
		protected BaseRecursiveService(
			final MockTCIServiceLoader mockTCIServiceLoader,
			final boolean async,
			final Class<?> clazz)
		{
			final Runnable run = () -> mockTCIServiceLoader.service(clazz);
			
			if(!async)
			{
				run.run();
				return;
			}
			
			final ExecutorService executor = Executors.newSingleThreadExecutor();
			try
			{
				CompletableFuture.runAsync(run, executor).join();
			}
			finally
			{
				executor.shutdown();
			}
		}
	}
	
	
	static class RecursiveServiceA extends BaseRecursiveService
	{
		RecursiveServiceA(final MockTCIServiceLoader mockTCIServiceLoader, final boolean async)
		{
			super(mockTCIServiceLoader, async, RecursiveServiceB.class);
		}
	}
	
	
	static class RecursiveServiceB extends BaseRecursiveService
	{
		RecursiveServiceB()
		{
		}
		
		RecursiveServiceB(final MockTCIServiceLoader mockTCIServiceLoader, final boolean async)
		{
			super(mockTCIServiceLoader, async, RecursiveServiceA.class);
		}
	}
	
	
	static class RecursiveServiceSelf extends BaseRecursiveService
	{
		RecursiveServiceSelf(final MockTCIServiceLoader mockTCIServiceLoader, final boolean async)
		{
			super(mockTCIServiceLoader, async, RecursiveServiceSelf.class);
		}
	}
}
