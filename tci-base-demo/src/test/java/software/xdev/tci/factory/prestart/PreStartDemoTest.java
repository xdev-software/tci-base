package software.xdev.tci.factory.prestart;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.dummyinfra.DummyTCI;
import software.xdev.tci.dummyinfra.factory.DummyTCIFactory;
import software.xdev.tci.factory.prestart.config.PreStartConfig;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.serviceloading.TCIServiceLoader;


class PreStartDemoTest
{
	private static final Logger LOG = LoggerFactory.getLogger(PreStartDemoTest.class);
	public static final int START_COUNT = 5;
	
	static DummyTCIFactory dummyFactory;
	
	@BeforeAll
	static void beforeAll()
	{
		// Force enable PreStarting
		TCIServiceLoader.instance().forceOverwrite(PreStartConfig.class, new PreStartConfig()
		{
			@Override
			public boolean enabled()
			{
				// set this to false and everything gets slow
				return true;
			}
			
			@Override
			public int keepReady(final String preStartName)
			{
				return this.enabled() ? START_COUNT : 0;
			}
			
			@Override
			public int maxStartSimultan(final String preStartName)
			{
				return this.enabled() ? START_COUNT : -1;
			}
		});
		
		dummyFactory = new DummyTCIFactory();
		
		TCIFactoryRegistry.instance().warmUp();
	}
	
	@SuppressWarnings({"java:S2699", "java:S2925"})
	@Test
	void showCasePreStarting() throws Exception
	{
		LOG.info("Waiting a moment until PreStarting is warmed up");
		Thread.sleep((START_COUNT + 1) * 1_000);
		
		for(int i = 0; i < START_COUNT; i++)
		{
			final long startTime = System.currentTimeMillis();
			final DummyTCI tci = dummyFactory.getNew(null);
			LOG.info("Time needed tp get infra: {}ms", System.currentTimeMillis() - startTime);
			
			tci.stop();
		}
	}
}
