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

import java.util.Objects;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;


/**
 * Manually executes a transaction.
 * <p/>
 * Can be used when no transaction framework (e.g. delivered with an AppSever) is present.
 */
public class TransactionExecutor
{
	protected final EntityManager em;
	
	public TransactionExecutor(final EntityManager em)
	{
		this.em = Objects.requireNonNull(em);
	}
	
	public void execWithTransaction(final Runnable run)
	{
		this.em.getTransaction().begin();
		
		try
		{
			run.run();
			this.em.getTransaction().commit();
		}
		catch(final Exception e)
		{
			this.em.getTransaction().rollback();
			throw e;
		}
	}
	
	public <T> T execWithTransaction(final Supplier<T> supplier)
	{
		this.em.getTransaction().begin();
		
		try
		{
			final T result = supplier.get();
			this.em.getTransaction().commit();
			
			return result;
		}
		catch(final Exception e)
		{
			this.em.getTransaction().rollback();
			throw e;
		}
	}
	
	public void close()
	{
		this.em.close();
	}
}
