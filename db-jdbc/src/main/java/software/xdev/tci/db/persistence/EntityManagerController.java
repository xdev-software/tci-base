/*
 * Copyright © 2025 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.tci.db.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles the creation and destruction of {@link EntityManager}s.
 * <p/>
 * This should only be used when a {@link EntityManager} has to be created manually, e.g. during tests.
 */
public class EntityManagerController implements AutoCloseable
{
	private static final Logger LOG = LoggerFactory.getLogger(EntityManagerController.class);
	
	protected final List<EntityManager> activeEms = Collections.synchronizedList(new ArrayList<>());
	protected final EntityManagerFactory emf;
	
	public EntityManagerController(final EntityManagerFactory emf)
	{
		this.emf = Objects.requireNonNull(emf);
	}
	
	/**
	 * Creates a new {@link EntityManager} with an internal {@link EntityManagerFactory}, which can be used to load and
	 * save data in the database.
	 *
	 * <p>
	 * All created EntityManager are automatically cleaned up once {@link #close()} is called.
	 * </p>
	 *
	 * @return EntityManager
	 */
	public EntityManager createEntityManager()
	{
		final EntityManager em = this.emf.createEntityManager();
		this.activeEms.add(em);
		
		return em;
	}
	
	@Override
	public void close()
	{
		LOG.info("Shutting down resources");
		this.activeEms.forEach(em ->
		{
			try
			{
				if(em.getTransaction() != null && em.getTransaction().isActive())
				{
					em.getTransaction().rollback();
				}
				
				// EclipseLink will crash if close is called on an already closed EntityManager
				if(em.isOpen())
				{
					em.close();
				}
			}
			catch(final Exception e)
			{
				LOG.warn("Unable to close EntityManager", e);
			}
		});
		
		LOG.info("Cleared {}x EntityManagers", this.activeEms.size());
		
		this.activeEms.clear();
		
		try
		{
			this.emf.close();
			LOG.info("Released EntityManagerFactory");
		}
		catch(final Exception e)
		{
			LOG.error("Failed to release EntityManagerFactory", e);
		}
	}
}
