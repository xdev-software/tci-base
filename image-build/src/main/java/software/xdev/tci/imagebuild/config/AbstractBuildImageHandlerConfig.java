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

import software.xdev.tci.config.DefaultConfig;


@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class AbstractBuildImageHandlerConfig extends DefaultConfig implements BuildImageHandlerConfig
{
	protected static final String DELETE_ON_EXIT = "delete-on-exit";
	protected static final String LOGGER_FOR_BUILD_PREFIX = "logger-for-build-prefix";
	protected static final String CACHE_FROM = "cache-from";
	protected static final String CACHE_TO = "cache-to";
	protected static final String SAVE_CACHE_IN_BACKGROUND = "save-cache-in-background";
	protected static final String WAIT_FOR_SAVE_CACHE_IN_BACKGROUND = "wait-for-" + SAVE_CACHE_IN_BACKGROUND;
	
	protected Boolean deleteOnExit;
	protected String loggerForBuildPrefix;
	protected Optional<String> cacheFrom;
	protected Optional<String> cacheTo;
	protected Boolean saveCacheInBackground;
	protected Boolean waitForSaveCacheInBackground;
}
