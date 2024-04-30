package software.xdev.tci.factory.prestart.loadbalancing;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


public class NodeExporterContainer extends GenericContainer<NodeExporterContainer>
{
	public static final DockerImageName DEFAULT_IMAGE =
		DockerImageName.parse("quay.io/prometheus/node-exporter:latest");
	public static final int PORT = 9100;
	
	public NodeExporterContainer()
	{
		this(DEFAULT_IMAGE);
	}
	
	public NodeExporterContainer(final DockerImageName dockerImageName)
	{
		super(dockerImageName);
		this.addExposedPort(PORT);
	}
	
	public String getExternalMetricsEndpoint()
	{
		return "http://" + this.getHost() + ":" + this.getMappedPort(PORT) + "/metrics";
	}
}
