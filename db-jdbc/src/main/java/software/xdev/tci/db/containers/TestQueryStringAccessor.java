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
package software.xdev.tci.db.containers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.testcontainers.containers.JdbcDatabaseContainer;


public final class TestQueryStringAccessor
{
	@SuppressWarnings("java:S3011")
	public static String testQueryString(final JdbcDatabaseContainer<?> container)
		throws InvocationTargetException, IllegalAccessException
	{
		Class<?> currentClass = container.getClass();
		while(currentClass != null && !JdbcDatabaseContainer.class.equals(currentClass))
		{
			try
			{
				final Method mGetTestQueryString = currentClass.getDeclaredMethod("getTestQueryString");
				mGetTestQueryString.setAccessible(true);
				return (String)mGetTestQueryString.invoke(container);
			}
			catch(final NoSuchMethodException ignored)
			{
				// Skip
			}
			currentClass = currentClass.getSuperclass();
		}
		return null;
	}
	
	private TestQueryStringAccessor()
	{
	}
}
