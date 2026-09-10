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
package software.xdev.tci.imagebuild;

import java.time.Duration;
import java.util.function.UnaryOperator;

import software.xdev.tci.serviceloading.TCIServiceLoaderHolder;
import software.xdev.testcontainers.imagebuilder.AdvancedImageFromDockerFile;
import software.xdev.testcontainers.imagebuilder.buildxnative.NativeAdvancedImageFromDockerfile;


public final class BuildImage
{
	public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
	
	public static String nativeImage(
		final String dockerImage,
		final UnaryOperator<NativeAdvancedImageFromDockerfile> configure)
	{
		return nativeImage(dockerImage, DEFAULT_TIMEOUT, configure);
	}
	
	public static String nativeImage(
		final String dockerImage,
		final Duration timeout,
		final UnaryOperator<NativeAdvancedImageFromDockerfile> configure)
	{
		return impl().nativeImage(dockerImage, timeout, configure);
	}
	
	public static String image(
		final String dockerImage,
		final UnaryOperator<AdvancedImageFromDockerFile> configure)
	{
		return image(dockerImage, DEFAULT_TIMEOUT, configure);
	}
	
	public static String image(
		final String dockerImage,
		final Duration timeout,
		final UnaryOperator<AdvancedImageFromDockerFile> configure)
	{
		return impl().image(dockerImage, timeout, configure);
	}
	
	public static BuildImageHandlerProvider impl()
	{
		return TCIServiceLoaderHolder.instance().service(BuildImageHandlerProvider.class);
	}
	
	private BuildImage()
	{
	}
}
