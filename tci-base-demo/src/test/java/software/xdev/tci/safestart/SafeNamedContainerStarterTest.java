package software.xdev.tci.safestart;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.dummyinfra.containers.DummyContainer;


class SafeNamedContainerStarterTest
{
	private static final Logger LOG = LoggerFactory.getLogger(SafeNamedContainerStarterTest.class);
	
	@SuppressWarnings({"java:S2699", "java:S2925"})
	@Test
	void test() throws Exception
	{
		final DummyContainer container = new DummyContainer();
		
		// Sadly simulating random container startup errors is not possible :(
		new SafeNamedContainerStarter<>("i-have-a-name", container).start();
		
		LOG.info("Name of container: {}", container.getContainerName().substring(1));
		
		Thread.sleep(10_000);
		
		container.stop();
	}
}
