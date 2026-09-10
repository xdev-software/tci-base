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
package software.xdev.tci.envperf.impl;

import java.util.Optional;

import org.slf4j.LoggerFactory;


public class DefaultTCIEnvironmentPerformance implements TCIEnvironmentPerformance
{
	public static final String ENV_SLOWNESS_FACTOR = "TCI_SLOWNESS_FACTOR";
	public static final String PROPERTY_SLOWNESS_FACTOR = "tci.slowness.factor";
	
	protected Integer slownessFactor;
	
	/**
	 * Describes the performance of the underlying environment.<br/> Higher values indicate a slower environment.<br/>
	 * Guideline is a follows:
	 * <ul>
	 *     <li>1 equals a standard developer machine CPU with roughly <code>16T 3GHz</code> or better</li>
	 *     <li>for a Raspberry PI 5 with <code>4T 2.4GHz</code> a value of roughly 3 should be chosen</li>
	 * </ul>
	 * The default value is 1.<br/>Min=1, Max=10
	 */
	@Override
	public int cpuSlownessFactor()
	{
		if(this.slownessFactor == null)
		{
			this.slownessFactor = this.parseSlownessFactor();
		}
		return this.slownessFactor;
	}
	
	protected synchronized int parseSlownessFactor()
	{
		return Optional.ofNullable(System.getenv(ENV_SLOWNESS_FACTOR))
			.or(() -> Optional.ofNullable(System.getProperty(PROPERTY_SLOWNESS_FACTOR)))
			.map(v -> {
				try
				{
					return Math.min(Math.max(Integer.parseInt(v), 1), 10);
				}
				catch(final Exception e)
				{
					LoggerFactory.getLogger(this.getClass()).error("Unable to parse", e);
					return null;
				}
			})
			.orElse(1);
	}
}
