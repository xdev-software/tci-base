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
package software.xdev.tci.imagebuild.config;

import java.util.Optional;


@SuppressWarnings({"java:S2789", "OptionalAssignedToNull"})
public class DefaultBuildImageHandlerConfig extends AbstractBuildImageHandlerConfig
{
	@Override
	protected String propertyNamePrefix()
	{
		return "tci.image-build";
	}
	
	@Override
	public boolean deleteOnExit()
	{
		if(this.deleteOnExit == null)
		{
			this.deleteOnExit = this.resolveBool(DELETE_ON_EXIT, false);
		}
		return this.deleteOnExit;
	}
	
	@Override
	public String loggerForBuildPrefix()
	{
		if(this.loggerForBuildPrefix == null)
		{
			this.loggerForBuildPrefix = this.resolve(LOGGER_FOR_BUILD_PREFIX)
				.orElse("container.build.");
		}
		return this.loggerForBuildPrefix;
	}
	
	@Override
	public Optional<String> cacheFrom()
	{
		if(this.cacheFrom == null)
		{
			this.cacheFrom = this.resolve(CACHE_FROM);
		}
		return this.cacheFrom;
	}
	
	@Override
	public Optional<String> cacheTo()
	{
		if(this.cacheTo == null)
		{
			this.cacheTo = this.resolve(CACHE_TO);
		}
		return this.cacheTo;
	}
	
	@Override
	public boolean saveCacheInBackground()
	{
		if(this.saveCacheInBackground == null)
		{
			this.saveCacheInBackground =
				this.resolveBool(SAVE_CACHE_IN_BACKGROUND, this.cacheTo().isPresent());
		}
		return this.saveCacheInBackground;
	}
	
	@Override
	public boolean waitForSaveCacheInBackground()
	{
		if(this.waitForSaveCacheInBackground == null)
		{
			this.waitForSaveCacheInBackground =
				this.resolveBool(WAIT_FOR_SAVE_CACHE_IN_BACKGROUND, true);
		}
		return this.waitForSaveCacheInBackground;
	}
}
