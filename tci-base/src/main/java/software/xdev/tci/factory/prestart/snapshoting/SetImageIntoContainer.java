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
package software.xdev.tci.factory.prestart.snapshoting;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.RemoteDockerImage;


public final class SetImageIntoContainer
{
	private static BiConsumer<GenericContainer<?>, RemoteDockerImage> instance;
	
	@SuppressWarnings({"java:S3011", "java:S112", "java:S1452"})
	public static BiConsumer<GenericContainer<?>, RemoteDockerImage> instance()
	{
		if(instance == null)
		{
			try
			{
				final Field fImage = GenericContainer.class.getDeclaredField("image");
				fImage.setAccessible(true);
				
				final Method mGetContainerDef = GenericContainer.class.getDeclaredMethod("getContainerDef");
				mGetContainerDef.setAccessible(true);
				
				final Class<?> containerDefClass = Class.forName("org.testcontainers.containers.ContainerDef");
				final Method mContainerDefSetImage =
					containerDefClass.getDeclaredMethod("setImage", RemoteDockerImage.class);
				mContainerDefSetImage.setAccessible(true);
				
				instance = (container, image) -> {
					try
					{
						fImage.set(container, image);
						mContainerDefSetImage.invoke(mGetContainerDef.invoke(container), image);
					}
					catch(final IllegalArgumentException | IllegalAccessException | InvocationTargetException ex2)
					{
						throw new RuntimeException(ex2);
					}
				};
			}
			catch(final NoSuchMethodException | NoSuchFieldException | ClassNotFoundException ex)
			{
				throw new RuntimeException(ex);
			}
		}
		
		return instance;
	}
	
	private SetImageIntoContainer()
	{
	}
}
