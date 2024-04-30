package software.xdev.tci.factory.prestart.coordinator;

import software.xdev.tci.factory.prestart.PreStartableTCIFactory;
import software.xdev.tci.serviceloading.TCIServiceLoader;


public interface GlobalPreStartCoordinator extends AutoCloseable
{
	void register(final PreStartableTCIFactory<?, ?> factory);
	
	void unregister(final PreStartableTCIFactory<?, ?> factory);
	
	@Override
	void close();
	
	static GlobalPreStartCoordinator instance()
	{
		return TCIServiceLoader.instance().service(GlobalPreStartCoordinator.class);
	}
	
	static boolean isPresent()
	{
		return TCIServiceLoader.instance().isLoaded(GlobalPreStartCoordinator.class);
	}
}
