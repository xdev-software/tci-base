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
package software.xdev.tci.db.persistence.hibernate;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;


@SuppressWarnings("java:S6548")
public class CachingStandardScanner extends StandardScanner
{
	private static CachingStandardScanner instance;
	
	public static CachingStandardScanner instance()
	{
		if(instance == null)
		{
			instance = new CachingStandardScanner();
		}
		return instance;
	}
	
	private final Map<ScanKey, ScanResult> cache = new ConcurrentHashMap<>();
	
	@Override
	public ScanResult scan(
		final ScanEnvironment environment,
		final ScanOptions options,
		final ScanParameters parameters)
	{
		return this.cache.computeIfAbsent(
			new ScanKey(environment, options, parameters),
			ignored -> super.scan(environment, options, parameters));
	}
	
	protected record ScanKey(
		URL getRootUrl,
		List<URL> getNonRootUrls,
		List<String> getExplicitlyListedClassNames,
		List<String> getExplicitlyListedMappingFiles,
		boolean canDetectUnlistedClassesInNonRoot,
		boolean canDetectUnlistedClassesInRoot,
		ScanParameters parameters)
	{
		protected ScanKey(
			final ScanEnvironment environment,
			final ScanOptions options,
			final ScanParameters parameters)
		{
			// Use components of methods because equals & hashCode are not overridden
			this(
				environment.getRootUrl(),
				environment.getNonRootUrls(),
				environment.getExplicitlyListedClassNames(),
				environment.getExplicitlyListedMappingFiles(),
				options.canDetectUnlistedClassesInNonRoot(),
				options.canDetectUnlistedClassesInRoot(),
				parameters
			);
		}
	}
}
