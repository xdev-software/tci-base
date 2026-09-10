package software.xdev.tci.portfixation;

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;

import com.github.dockerjava.api.model.Ports;

import software.xdev.tci.dummyinfra.containers.DummyContainer;
import software.xdev.tci.network.LazyNetwork;


class PortFixationLiveTest
{
	private static final Logger LOG = LoggerFactory.getLogger(PortFixationLiveTest.class);
	
	@SuppressWarnings({"resource", "java:S2699", "java:S2925"})
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void showCase(final boolean withFixation)
	{
		try(final Network network = new LazyNetwork()
			.withName("12b77690-fffb-434f-b3b3-ae2c97e14aa6")
			.create();
			final DummyContainer container = new DummyContainer())
		{
			if(withFixation)
			{
				PortFixation.makeExposedPortsFix(container);
			}
			
			container.start();
			
			final Integer expectedPort = container.getMappedPort(DummyContainer.PORT);
			
			DockerClientFactory.lazyClient().connectToNetworkCmd()
				.withNetworkId(network.getId())
				.withContainerId(container.getContainerId())
				.exec();
			
			final boolean containsExpectedPort = DockerClientFactory.lazyClient()
				.inspectContainerCmd(container.getContainerId())
				.exec()
				.getNetworkSettings()
				.getPorts()
				.getBindings()
				.values()
				.stream()
				.flatMap(Arrays::stream)
				.map(Ports.Binding::getHostPortSpec)
				.map(p -> {
					try
					{
						return Integer.parseInt(p);
					}
					catch(final NumberFormatException ex)
					{
						return null;
					}
				})
				.filter(Objects::nonNull)
				.mapToInt(i -> i)
				.anyMatch(port -> Objects.equals(port, expectedPort));
			
			LOG.info("Can container be reached from host? - {}", containsExpectedPort);
			
			// Only when fixation is active the port is exposed after connect!
			Assertions.assertEquals(withFixation, containsExpectedPort);
		}
	}
}
