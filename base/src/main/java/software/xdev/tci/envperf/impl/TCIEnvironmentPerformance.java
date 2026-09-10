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

public interface TCIEnvironmentPerformance
{
	/**
	 * Describes the performance of the underlying environment.<br/> Higher values indicate a slower environment.<br/>
	 * Guideline is a follows:
	 * <ul>
	 *     <li>1 equals a standard developer machine CPU with roughly <code>16T 3GHz</code> or better</li>
	 *     <li>for a Raspberry PI 5 with <code>4T 2.4GHz</code> a value of roughly 3 should be chosen</li>
	 * </ul>
	 * The default value is 1.<br/>Min=1, Max=10
	 */
	int cpuSlownessFactor();
}
