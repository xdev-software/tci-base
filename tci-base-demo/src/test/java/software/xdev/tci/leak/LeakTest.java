package software.xdev.tci.leak;

import org.junit.jupiter.api.Test;

import software.xdev.tci.dummyinfra.DummyTCI;
import software.xdev.tci.dummyinfra.factory.DummyTCIFactory;


class LeakTest
{
	static final DummyTCIFactory DUMMY_FACTORY = new DummyTCIFactory();
	
	@SuppressWarnings({"java:S2699"})
	@Test
	void createLeak()
	{
		final DummyTCI tci = DUMMY_FACTORY.getNew(null);
		
		// Imagine doing some testing here
		
		// Forget to close/stop tci -> After the test is finished a warning will be visible in the logs!
		// It can be fixed by commenting in the following code:
		// tci.stop();
	}
}
