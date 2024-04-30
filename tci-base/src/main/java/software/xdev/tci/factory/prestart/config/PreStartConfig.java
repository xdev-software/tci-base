package software.xdev.tci.factory.prestart.config;

import software.xdev.tci.serviceloading.TCIServiceLoader;


public interface PreStartConfig
{
	boolean enabled();
	
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
	boolean directNetworkAttachIfPossible(final String preStartName);
	
	/**
	 * Amount of CPU that needs to be idle to allow PreStarting of containers.
	 */
	int coordinatorIdleCPUPercent();
	
	/**
	 * How often PreStarting should be tried (one factory per schedule!)
	 */
	int coordinatorSchedulePeriodMs();
	
	/**
	 * Should PreStarting be stopped when tests are ending?
	 * <p>
	 * DEBUG-Option - default value should be fine.
	 * </p>
	 */
	boolean detectEndingTests();
	
	static PreStartConfig instance()
	{
		return TCIServiceLoader.instance().service(PreStartConfig.class);
	}
}
