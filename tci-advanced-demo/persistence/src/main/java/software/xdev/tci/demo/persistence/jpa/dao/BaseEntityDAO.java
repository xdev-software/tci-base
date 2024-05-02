package software.xdev.tci.demo.persistence.jpa.dao;

import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import software.xdev.tci.demo.entities.IdentifiableEntity;


public abstract class BaseEntityDAO<T extends IdentifiableEntity> extends BaseDAO
{
	protected BaseEntityDAO()
	{
	}
	
	protected BaseEntityDAO(final EntityManager em)
	{
		this.setEntityManager(em);
	}
	
	@Transactional
	public T save(final T entity)
	{
		Objects.requireNonNull(entity);
		
		final EntityManager entityManager = this.getEntityManager();
		
		if(entity.getId() == 0L)
		{
			entityManager.persist(entity);
		}
		
		final T mergedEntity = entityManager.merge(entity);
		
		entityManager.flush();
		
		return mergedEntity;
	}
	
	protected T getById(final Class<T> type, final long id)
	{
		return this.getEntityManager().find(type, id);
	}
}
