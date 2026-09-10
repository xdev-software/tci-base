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
package software.xdev.tci.imagebuild.handler;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.imagebuild.cache.async.TCICacheSaveRegistry;
import software.xdev.tci.imagebuild.config.BuildImageHandlerConfig;
import software.xdev.testcontainers.imagebuilder.buildxnative.NativeAdvancedImageFromDockerfile;


public class NativeAdvancedBuildImageHandler extends AbstractBuildImageHandler<NativeAdvancedImageFromDockerfile>
{
	@Override
	protected String build(
		final String dockerImage,
		final String sanitizedDockerImageName,
		final BuildImageHandlerConfig config,
		final Duration timeout,
		final UnaryOperator<NativeAdvancedImageFromDockerfile> configure)
	{
		this.logger.info("Starting build of (native) image {}", dockerImage);
		
		final NativeAdvancedImageFromDockerfile builder =
			new NativeAdvancedImageFromDockerfile(dockerImage, config.deleteOnExit());
		
		this.configureAbstract(builder, config, sanitizedDockerImageName);
		
		this.templateCacheConfigValue(config.cacheFrom(), sanitizedDockerImageName)
			.ifPresent(builder::withCacheFrom);
		
		if(config.saveCacheInBackground())
		{
			builder.withCreateTransferFilesCache(true);
		}
		else
		{
			this.configureCacheTo(sanitizedDockerImageName, config, builder);
		}
		
		final NativeAdvancedImageFromDockerfile customizedBuilder = configure.apply(builder);
		
		final String builtImageName = this.buildImage(customizedBuilder, timeout);
		
		if(config.saveCacheInBackground() && config.cacheTo().isPresent())
		{
			this.logger.info("Rebuilding image {} in background to save cache", builtImageName);
			TCICacheSaveRegistry.instance().add(
				CompletableFuture.runAsync(
					() -> {
						try
						{
							this.buildImage(
								this.configureCacheTo(
									sanitizedDockerImageName,
									config,
									customizedBuilder.copyForExactRebuild(dockerImage + "-cache")
										// Don't load it into docker because it's not needed there
										.withLoad(false)),
								timeout);
						}
						catch(final Exception ex)
						{
							this.logger.warn("Rebuilding {} to save cache failed", builtImageName, ex);
						}
						finally
						{
							try
							{
								builder.cleanCreatedTransferFilesCache();
							}
							catch(final Exception ex)
							{
								this.logger.warn(
									"Failed to clean created transfer file cache for {}",
									builtImageName,
									ex);
							}
						}
					},
					TCIExecutorServiceHolder.instance()));
		}
		
		return builtImageName;
	}
	
	protected NativeAdvancedImageFromDockerfile configureCacheTo(
		final String sanitizeDockerImageName,
		final BuildImageHandlerConfig config,
		final NativeAdvancedImageFromDockerfile builder)
	{
		this.templateCacheConfigValue(config.cacheTo(), sanitizeDockerImageName)
			.ifPresent(builder::withCacheTo);
		return builder;
	}
	
	protected Optional<String> templateCacheConfigValue(
		final Optional<String> configValue,
		final String sanitizeDockerImageName)
	{
		return configValue
			.map(s -> s.replace("$image", sanitizeDockerImageName))
			.map(s -> s.replace("§image", sanitizeDockerImageName));
	}
}
