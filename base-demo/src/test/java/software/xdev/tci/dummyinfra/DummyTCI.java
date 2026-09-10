package software.xdev.tci.dummyinfra;

import software.xdev.tci.TCI;
import software.xdev.tci.dummyinfra.containers.DummyContainer;


public class DummyTCI extends TCI<DummyContainer>
{
	public DummyTCI(final DummyContainer container, final String networkAlias)
	{
		super(container, networkAlias);
	}
}
