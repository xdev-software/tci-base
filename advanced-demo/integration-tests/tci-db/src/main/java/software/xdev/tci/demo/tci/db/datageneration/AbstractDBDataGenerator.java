package software.xdev.tci.demo.tci.db.datageneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import jakarta.persistence.EntityManager;

import software.xdev.tci.db.datageneration.BaseDBDataGenerator;
import software.xdev.tci.db.persistence.TransactionExecutor;
import software.xdev.tci.demo.entities.IdentifiableEntity;
import software.xdev.tci.demo.persistence.jpa.dao.BaseEntityDAO;


/**
 * Base class for all data generators. Holds an {@link EntityManager} and a {@link TransactionExecutor} to save data.
 *
 * @author AB
 */
public abstract class AbstractDBDataGenerator extends BaseDBDataGenerator
{
	protected AbstractDBDataGenerator(final EntityManager em)
	{
		super(em);
	}
	
	protected AbstractDBDataGenerator(final EntityManager em, final TransactionExecutor transactor)
	{
		super(em, transactor);
	}
	
	@SafeVarargs
	public final <T extends IdentifiableEntity> List<T> saveBatch(
		final Function<EntityManager, BaseEntityDAO<T>> daoSupplier,
		final T... elements)
	{
		return this.transactor()
			.execWithTransaction(
				() -> daoSupplier.apply(this.em()).saveBatch(new ArrayList<>(Arrays.asList(elements))));
	}
	
	public <T extends IdentifiableEntity> List<T> saveBatch(
		final Function<EntityManager, BaseEntityDAO<T>> daoSupplier,
		final Collection<T> elements)
	{
		return this.transactor()
			.execWithTransaction(() -> daoSupplier.apply(this.em()).saveBatch(elements));
	}
	
	public <T extends IdentifiableEntity> T save(
		final Function<EntityManager, BaseEntityDAO<T>> daoSupplier,
		final T element)
	{
		return this.transactor().execWithTransaction(() -> daoSupplier.apply(this.em()).save(element));
	}
}
