package software.xdev.tci.leak;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.dummyinfra.DummyTCI;
import software.xdev.tci.dummyinfra.factory.DummyTCIFactory;


class LeakTest
{
	private static final Logger LOG = LoggerFactory.getLogger(LeakTest.class);
	
	static final DummyTCIFactory DUMMY_FACTORY = new DummyTCIFactory();
	
	@SuppressWarnings({"java:S2699"})
	@Test
	void createLeak()
	{
		final DummyTCI tci = DUMMY_FACTORY.getNew(null);
		
		// Imagine doing some testing here
		LOG.info("ContainerId: {}", tci.getContainer().getContainerId());
		
		// Forget to close/stop tci -> After the test is finished a warning will be visible in the logs!
		// It can be fixed by commenting in the following code:
		// tci.stop();
	}
}
