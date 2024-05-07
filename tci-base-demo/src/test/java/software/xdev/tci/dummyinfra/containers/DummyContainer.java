package software.xdev.tci.dummyinfra.containers;

import org.testcontainers.containers.GenericContainer;


public class DummyContainer extends GenericContainer<DummyContainer>
{
	public static final int PORT = 80;
	
	public DummyContainer()
	{
		super("nginx:stable-alpine");
		this.addExposedPort(PORT);
	}
}
