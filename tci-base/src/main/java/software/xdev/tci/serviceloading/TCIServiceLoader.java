package software.xdev.tci.serviceloading;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;


/**
 * Central point for service loading
 */
public final class TCIServiceLoader
{
	private static final TCIServiceLoader INSTANCE = new TCIServiceLoader();
	
	public static TCIServiceLoader instance()
	{
		return INSTANCE;
	}
	
	private final Map<Class<?>, Object> loadedServices = Collections.synchronizedMap(new HashMap<>());
	
	private TCIServiceLoader()
	{
	}
	
	@SuppressWarnings("unchecked")
	public <T> T service(final Class<T> clazz)
	{
		return (T)this.loadedServices.computeIfAbsent(
			clazz,
			c -> ServiceLoader.load(clazz)
				.stream()
				// Get by highest priority
				.max(Comparator.comparingInt(p ->
					Optional.ofNullable(p.type().getAnnotation(TCIProviderPriority.class))
						.map(TCIProviderPriority::value)
						.orElse(TCIProviderPriority.DEFAULT_PRIORITY)))
				.map(ServiceLoader.Provider::get)
				.orElse(null));
	}
	
	public boolean isLoaded(final Class<?> clazz)
	{
		return this.loadedServices.get(clazz) != null;
	}
}
