package software.xdev.tci.factory;

import java.util.Set;

import org.testcontainers.containers.GenericContainer;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.tracing.TCITracer;


/**
 * A factory for {@link TCI}
 */
public interface TCIFactory<C extends GenericContainer<C>, I extends TCI<C>> extends AutoCloseable
{
	default void register()
	{
		TCIFactoryRegistry.instance().register(this);
	}
	
	default void unregister()
	{
		TCIFactoryRegistry.instance().unRegister(this);
	}
	
	void warmUp();
	
	@Override
	void close();
	
	Set<I> getReturnedAndInUse();
	
	default String getFactoryName()
	{
		return this.getClass().getSimpleName();
	}
	
	TCITracer getTracer();
}
