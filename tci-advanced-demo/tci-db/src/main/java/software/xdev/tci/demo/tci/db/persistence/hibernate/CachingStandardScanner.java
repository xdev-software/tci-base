package software.xdev.tci.demo.tci.db.persistence.hibernate;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;


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
	
	record ScanKey(
		URL getRootUrl,
		List<URL> getNonRootUrls,
		List<String> getExplicitlyListedClassNames,
		List<String> getExplicitlyListedMappingFiles,
		boolean canDetectUnlistedClassesInNonRoot,
		boolean canDetectUnlistedClassesInRoot,
		ScanParameters parameters)
	{
		public ScanKey(
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
