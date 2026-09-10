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
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.imagebuild.config.BuildImageHandlerConfig;
import software.xdev.tci.imagebuild.config.OverlayBuildImageHandlerConfig;
import software.xdev.testcontainers.imagebuilder.AbstractImageFromDockerfile;


public abstract class AbstractBuildImageHandler<I extends AbstractImageFromDockerfile<I>>
	implements BuildImageHandler<I>
{
	protected static final Pattern DOCKER_IMAGE_SANITIZATION_PATTERN = Pattern.compile("[^A-Za-z0-9-_]");
	
	protected Logger logger;
	
	protected AbstractBuildImageHandler()
	{
		this.logger = LoggerFactory.getLogger(this.getClass());
	}
	
	@Override
	public String build(
		final String dockerImage,
		final BuildImageHandlerConfig parentConfig,
		final Duration timeout,
		final UnaryOperator<I> configure)
	{
		final String sanitizedDockerImageName = this.sanitizeDockerImageName(dockerImage);
		return this.build(
			dockerImage,
			sanitizedDockerImageName,
			new OverlayBuildImageHandlerConfig(parentConfig, sanitizedDockerImageName),
			timeout,
			configure);
	}
	
	protected abstract String build(
		final String dockerImage,
		final String sanitizedDockerImageName,
		final BuildImageHandlerConfig parentConfig,
		final Duration timeout,
		final UnaryOperator<I> configure);
	
	protected void configureAbstract(
		final I builder,
		final BuildImageHandlerConfig config,
		final String sanitizeDockerImageName)
	{
		builder.withLoggerForBuild(
			LoggerFactory.getLogger(config.loggerForBuildPrefix() + sanitizeDockerImageName));
	}
	
	protected String buildImage(
		final AbstractImageFromDockerfile<?> builder,
		final Duration timeout)
	{
		this.logger.info("Building image {}...", builder.getDockerImageName());
		final String builtImageName = builder.build(timeout);
		this.logger.info("Built image {}", builtImageName);
		
		return builtImageName;
	}
	
	protected String sanitizeDockerImageName(final String dockerImage)
	{
		return DOCKER_IMAGE_SANITIZATION_PATTERN.matcher(dockerImage).replaceAll("_");
	}
}
