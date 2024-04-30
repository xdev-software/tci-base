package software.xdev.tci.factory.registry;

import java.util.Map;
import java.util.Set;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.TCIFactory;
import software.xdev.tci.serviceloading.TCIServiceLoader;


/**
 * Registry for all factories that create infrastructure.
 * <p>
 * e.g. used by various agents to enumerate all factories
 * </p>
 */
@SuppressWarnings("java:S1452")
public interface TCIFactoryRegistry
{
	void register(final TCIFactory<?, ?> tciFactory);
	
	void unRegister(final TCIFactory<?, ?> tciFactory);
	
	void warmUp();
	
	Set<TCIFactory<?, ?>> getFactories();
	
	Map<TCIFactory<?, ?>, Set<TCI<?>>> getReturnedAndInUse();
	
	static TCIFactoryRegistry instance()
	{
		return TCIServiceLoader.instance().service(TCIFactoryRegistry.class);
	}
}
