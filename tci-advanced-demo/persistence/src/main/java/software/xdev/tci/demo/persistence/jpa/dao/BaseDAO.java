package software.xdev.tci.demo.persistence.jpa.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;

import org.springframework.beans.factory.annotation.Autowired;


public abstract class BaseDAO
{
	@Autowired
	EntityManager em;
	
	public EntityManager getEntityManager()
	{
		return this.em;
	}
	
	public CriteriaBuilder getCriteriaBuilder()
	{
		return this.getEntityManager().getCriteriaBuilder();
	}
	
	void setEntityManager(final EntityManager em)
	{
		this.em = em;
	}
}
