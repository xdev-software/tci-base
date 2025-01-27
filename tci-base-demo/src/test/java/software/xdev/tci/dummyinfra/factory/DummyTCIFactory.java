package software.xdev.tci.dummyinfra.factory;

import software.xdev.tci.dummyinfra.DummyTCI;
import software.xdev.tci.dummyinfra.containers.DummyContainer;
import software.xdev.tci.factory.prestart.PreStartableTCIFactory;


public class DummyTCIFactory extends PreStartableTCIFactory<DummyContainer, DummyTCI>
{
	public DummyTCIFactory()
	{
		super(
			DummyTCI::new,
			DummyContainer::new,
			"dummy",
			"container.dummy",
			"Dummy");
	}
}
