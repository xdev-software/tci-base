package software.xdev.tci.safestart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.dummyinfra.containers.DummyContainer;


class SafeNamedContainerStarterTest
{
	private static final Logger LOG = LoggerFactory.getLogger(SafeNamedContainerStarterTest.class);
	
	private static final String CONTAINER_BASE_NAME = "i-have-a-name";
	
	@SuppressWarnings("java:S2925")
	@Test
	void test() throws Exception
	{
		final DummyContainer container = new DummyContainer();
		
		// Sadly simulating random container startup errors is not possible :(
		new SafeNamedContainerStarter<>(CONTAINER_BASE_NAME, container).start();
		
		final String containerName = container.getContainerName().substring(1);
		LOG.info("Name of container: {}", containerName);
		
		Assertions.assertTrue(containerName.startsWith(CONTAINER_BASE_NAME));
		Assertions.assertTrue(containerName.length() > CONTAINER_BASE_NAME.length());
		
		Thread.sleep(5_000);
		
		container.stop();
	}
}
