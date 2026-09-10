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

import software.xdev.tci.imagebuild.config.BuildImageHandlerConfig;
import software.xdev.tci.imagebuild.handler.AdvancedBuildImageHandler;
import software.xdev.tci.imagebuild.handler.NativeAdvancedBuildImageHandler;
import software.xdev.testcontainers.imagebuilder.AdvancedImageFromDockerFile;
import software.xdev.testcontainers.imagebuilder.buildxnative.NativeAdvancedImageFromDockerfile;


public class DefaultBuildImageHandlerProvider implements BuildImageHandlerProvider
{
	protected final BuildImageHandlerConfig config;
	
	public DefaultBuildImageHandlerProvider()
	{
		this(BuildImageHandlerConfig.instance());
	}
	
	public DefaultBuildImageHandlerProvider(final BuildImageHandlerConfig config)
	{
		this.config = config;
	}
	
	@Override
	public String nativeImage(
		final String dockerImage,
		final Duration timeout,
		final UnaryOperator<NativeAdvancedImageFromDockerfile> configure)
	{
		return new NativeAdvancedBuildImageHandler().build(
			dockerImage,
			this.config,
			timeout,
			configure);
	}
	
	@Override
	public String image(
		final String dockerImage,
		final Duration timeout,
		final UnaryOperator<AdvancedImageFromDockerFile> configure)
	{
		return new AdvancedBuildImageHandler().build(
			dockerImage,
			this.config,
			timeout,
			configure);
	}
}
