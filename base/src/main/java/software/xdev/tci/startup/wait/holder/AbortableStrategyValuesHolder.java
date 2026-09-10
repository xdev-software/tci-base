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
package software.xdev.tci.startup.wait.holder;

/**
 * @apiNote Please note that this is the Java 21 version. The Java 25 version uses ScopedValues.
 */
public final class AbortableStrategyValuesHolder
{
	private static final ThreadLocal<AbortableStrategyValues> TL = new ThreadLocal<>();
	
	public static void executeWith(final Runnable runnable, final AbortableStrategyValues values)
	{
		TL.set(values);
		try
		{
			runnable.run();
		}
		finally
		{
			TL.remove();
		}
	}
	
	public static AbortableStrategyValues get()
	{
		return TL.get();
	}
	
	private AbortableStrategyValuesHolder()
	{
	}
}
