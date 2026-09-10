package software.xdev.tci.demo.tci.db;

import java.util.Set;
import java.util.function.Supplier;

import software.xdev.tci.db.persistence.EntityManagerControllerFactory;


@FunctionalInterface
public interface EntityManagerControllerFactoryProvider
{
	EntityManagerControllerFactory<?, ?> create(Supplier<Set<String>> entityClassesFinder);
}
