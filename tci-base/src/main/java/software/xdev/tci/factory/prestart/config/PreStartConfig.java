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
package software.xdev.tci.factory.prestart.config;

import software.xdev.tci.serviceloading.TCIServiceLoader;


public interface PreStartConfig
{
	boolean DEFAULT_ENABLED = false;
	boolean DEFAULT_DIRECT_NETWORK_ATTACH_IF_POSSIBLE = true;
	int DEFAULT_COORDINATOR_IDLE_CPU_PERCENT = 40;
	int DEFAULT_COORDINATOR_SCHEDULE_PERIOD_MS = 1_000;
	boolean DEFAULT_DETECT_ENDING_TESTS = true;
	
	default boolean enabled()
	{
		return DEFAULT_ENABLED;
	}
	
	/**
	 * How many infrastructures s
	 */
	int keepReady(final String preStartName);
	
	int maxStartSimultan(final String preStartName);
	
	/**
	 * Tries to directly attach the container to the network if possible.
	 * <p>
	 * This speeds up overall time but (as no <code>docker network connect</code> is needed) but it's a different
	 * behavior then PreStarting (if implemented incorrectly may cause "random" problems)
	 * </p>
	 * <p>
	 * DEBUG-Option - default value should be fine.
	 * </p>
	 */
	default boolean directNetworkAttachIfPossible(final String preStartName)
	{
		return DEFAULT_DIRECT_NETWORK_ATTACH_IF_POSSIBLE;
	}
	
	/**
	 * Amount of CPU that needs to be idle to allow PreStarting of containers.
	 */
	default int coordinatorIdleCPUPercent()
	{
		return DEFAULT_COORDINATOR_IDLE_CPU_PERCENT;
	}
	
	/**
	 * How often PreStarting should be tried (one factory per schedule!)
	 */
	default int coordinatorSchedulePeriodMs()
	{
		return DEFAULT_COORDINATOR_SCHEDULE_PERIOD_MS;
	}
	
	/**
	 * Should PreStarting be stopped when tests are ending?
	 * <p>
	 * DEBUG-Option - default value should be fine.
	 * </p>
	 */
	default boolean detectEndingTests()
	{
		return this.enabled() && DEFAULT_DETECT_ENDING_TESTS;
	}
	
	static PreStartConfig instance()
	{
		return TCIServiceLoader.instance().service(PreStartConfig.class);
	}
}
