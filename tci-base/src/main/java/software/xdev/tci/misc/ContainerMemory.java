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
package software.xdev.tci.misc;

public final class ContainerMemory
{
	// NOTE: Variablenames can't start with numbers -> Prefixed with M
	// Docker uses 1024 as conversion
	public static final long M64M = 128 * 1024L * 1024L;
	public static final long M128M = M64M * 2;
	public static final long M256M = M128M * 2;
	public static final long M512M = M256M * 2;
	public static final long M1G = M512M * 2;
	public static final long M2G = M1G * 2;
	public static final long M4G = M2G * 2;
	public static final long M8G = M2G * 2;
	
	private ContainerMemory()
	{
	}
}
