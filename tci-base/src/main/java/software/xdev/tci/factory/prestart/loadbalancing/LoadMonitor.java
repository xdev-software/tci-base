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
