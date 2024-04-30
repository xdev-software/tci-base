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
package software.xdev.tci.factory.prestart.loadbalancing;

import java.util.OptionalDouble;

import software.xdev.tci.serviceloading.TCIServiceLoader;


/**
 * Monitors the load of the container environment.
 */
public interface LoadMonitor
{
	/**
	 * Idle load in percent. 12.34=12.34%; 0-100
	 * <p>
	 * {@link OptionalDouble#empty()} when initializing
	 * </p>
	 */
	OptionalDouble getCurrentIdlePercent();
	
	static LoadMonitor instance()
	{
		return TCIServiceLoader.instance().service(LoadMonitor.class);
	}
}
