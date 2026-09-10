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
package software.xdev.tci.concurrent;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;


/**
 * Based on
 * <a href="https://github.com/google/guava/blob/v33.6.0/guava/src/com/google/common/base/Suppliers.java">
 * Guava Supplier
 * </a>
 * and minimized.
 */
public final class Suppliers
{
	private Suppliers()
	{
	}
	
	/**
	 * Returns a supplier which caches the instance retrieved during the first call to {@code get()} and returns that
	 * value on subsequent calls to {@code get()}. See: <a
	 * href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
	 *
	 * <p>The returned supplier is thread-safe. The delegate's {@code get()} method will be invoked at
	 * most once unless the underlying {@code get()} throws an exception. The supplier's serialized form does not
	 * contain the cached value, which will be recalculated when {@code get()} is called on the deserialized instance.
	 *
	 * <p>When the underlying delegate throws an exception then this memoizing supplier will keep
	 * delegating calls until it returns valid data.
	 *
	 * <p>If {@code delegate} is an instance created by an earlier call to {@code memoize}, it is
	 * returned directly.
	 */
	public static <T> Supplier<T> memoize(final Supplier<T> delegate)
	{
		if(delegate instanceof NonSerializableMemoizingSupplier)
		{
			return delegate;
		}
		return new NonSerializableMemoizingSupplier<>(delegate);
	}
	
	@SuppressWarnings("java:S3077")
	static class NonSerializableMemoizingSupplier<T> implements Supplier<T>
	{
		private final ReentrantLock lock = new ReentrantLock();
		
		@SuppressWarnings("UnnecessaryLambda") // Must be a fixed singleton object
		private static final Supplier<Void> SUCCESSFULLY_COMPUTED =
			() -> {
				throw new IllegalStateException(); // Should never get called.
			};
		
		@SuppressWarnings("PMD.AvoidUsingVolatile")
		private volatile Supplier<T> delegate;
		// "value" does not need to be volatile; visibility piggy-backs on volatile read of "delegate".
		private T value;
		
		NonSerializableMemoizingSupplier(final Supplier<T> delegate)
		{
			this.delegate = Objects.requireNonNull(delegate);
		}
		
		@Override
		@SuppressWarnings("unchecked") // Cast from Supplier<Void> to Supplier<T> is always valid
		public T get()
		{
			// Because Supplier is read-heavy, we use the "double-checked locking" pattern.
			if(this.delegate != SUCCESSFULLY_COMPUTED)
			{
				this.lock.lock();
				try
				{
					if(this.delegate != SUCCESSFULLY_COMPUTED)
					{
						final T t = this.delegate.get();
						this.value = t;
						this.delegate = (Supplier<T>)SUCCESSFULLY_COMPUTED;
						return t;
					}
				}
				finally
				{
					this.lock.unlock();
				}
			}
			// This is safe because we checked `delegate`.
			return this.value;
		}
		
		@Override
		public String toString()
		{
			final Supplier<T> currentDelegate = this.delegate;
			return "Suppliers.memoize("
				+ (currentDelegate == SUCCESSFULLY_COMPUTED
				? "<supplier that returned " + this.value + ">"
				: currentDelegate)
				+ ")";
		}
	}
}
