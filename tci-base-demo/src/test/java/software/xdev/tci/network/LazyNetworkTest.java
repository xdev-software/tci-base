package software.xdev.tci.network;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;


/**
 * Showcases {@link LazyNetwork}
 */
class LazyNetworkTest
{
	private static final Logger LOG = LoggerFactory.getLogger(LazyNetworkTest.class);
	
	static final LazyNetworkPool NETWORK_POOL = new LazyNetworkPool(1);
	
	@BeforeAll
	static void beforeAll()
	{
		printAllNetworks();
		
		// Warm up pool and start creating networks in the background
		NETWORK_POOL.managePoolAsync();
	}
	
	public static Stream<Arguments> networkComparison()
	{
		return Stream.<Supplier<Network>>of(
			Network::newNetwork,
			NETWORK_POOL::getNew
		).map(Arguments::of);
	}
	
	@SuppressWarnings({"java:S2699", "java:S2925"})
	@ParameterizedTest
	@MethodSource
	void networkComparison(final Supplier<Network> networkSupplier) throws Exception
	{
		final long stopStartTime;
		try(final Network network = networkSupplier.get())
		{
			LOG.info("Network impl is {}", network.getClass());
			
			final long startTime = System.currentTimeMillis();
			network.getId();
			LOG.info(
				"Time required to get network id: {}ms; id is {}",
				System.currentTimeMillis() - startTime,
				network.getId());
			
			stopStartTime = System.currentTimeMillis();
		}
		
		LOG.info("Time required to close network: {}ms", System.currentTimeMillis() - stopStartTime);
		
		// Sleep a moment to cool down
		Thread.sleep(500);
		
		printAllNetworks();
	}
	
	static void printAllNetworks()
	{
		LOG.info("== ALL NETWORKS ===");
		DockerClientFactory.lazyClient().listNetworksCmd().exec().forEach(n -> {
			LOG.info("{} {}", n.getName(), n.getId());
		});
		LOG.info("");
	}
}
