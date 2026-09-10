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
package software.xdev.tci.leakdetection.config;

import java.time.Duration;

import software.xdev.tci.config.DefaultConfig;


public class DefaultLeakDetectionConfig extends DefaultConfig implements LeakDetectionConfig
{
	protected final boolean enabled;
	protected final Duration defaultStopTimeout;
	
	public DefaultLeakDetectionConfig()
	{
		this.enabled = this.resolveBool("enabled", DEFAULT_ENABLED);
		this.defaultStopTimeout = Duration.ofMillis(
			this.resolveLong("stop-timeout-ms", DEFAULT_STOP_TIMEOUT::toMillis));
	}
	
	@Override
	protected String propertyNamePrefix()
	{
		return "tci.leak-detection";
	}
	
	@Override
	public boolean enabled()
	{
		return this.enabled;
	}
	
	@Override
	public Duration defaultStopTimeout()
	{
		return this.defaultStopTimeout;
	}
}
