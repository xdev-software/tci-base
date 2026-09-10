package software.xdev.tci.demo.tci.db.eclipselink;

import java.util.Set;
import java.util.function.Supplier;

import software.xdev.tci.db.persistence.EntityManagerControllerFactory;
import software.xdev.tci.db.persistence.eclipselink.EclipseLinkEntityManagerControllerFactory;
import software.xdev.tci.demo.tci.db.EntityManagerControllerFactoryProvider;


public class EclipseLinkEntityManagerControllerFactoryProvider
	implements EntityManagerControllerFactoryProvider
{
	@Override
	public EntityManagerControllerFactory<?, ?> create(final Supplier<Set<String>> entityClassesFinder)
	{
		return new EclipseLinkEntityManagerControllerFactory(entityClassesFinder);
	}
}
