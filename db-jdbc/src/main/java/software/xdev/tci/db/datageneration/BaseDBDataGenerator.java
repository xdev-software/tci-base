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
package software.xdev.tci.db.datageneration;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.EntityManager;

import software.xdev.tci.db.persistence.TransactionExecutor;


/**
 * Base class for all data generators. Holds an {@link EntityManager} and a {@link TransactionExecutor} to save data.
 *
 * @implNote Due to generics save Methods need to be implemented downstream
 */
public abstract class BaseDBDataGenerator implements DataGenerator
{
	protected final EntityManager em;
	protected final TransactionExecutor transactor;
	
	protected BaseDBDataGenerator(final EntityManager em)
	{
		this(em, null);
	}
	
	protected BaseDBDataGenerator(final EntityManager em, final TransactionExecutor transactor)
	{
		this.em = Objects.requireNonNull(em, "EntityManager can't be null!");
		this.transactor = transactor != null ? transactor : new TransactionExecutor(em);
	}
	
	/**
	 * Returns the {@link EntityManager}-Instance of this generator, which can be used to save data.
	 */
	@SuppressWarnings("java:S1845") // Record style access
	protected EntityManager em()
	{
		return this.em;
	}
	
	/**
	 * Returns the {@link TransactionExecutor}-Instance of this generator, which can be used to save data with a
	 * transaction.
	 */
	@SuppressWarnings("java:S1845") // Record style access
	protected TransactionExecutor transactor()
	{
		return this.transactor;
	}
	
	/**
	 * Returns a {@link LocalDate} in the past. By default, 1970-01-01.
	 */
	@SuppressWarnings("checkstyle:MagicNumber")
	public LocalDate getLocalDateInPast()
	{
		return LocalDate.of(1970, 1, 1);
	}
	
	/**
	 * Returns a {@link LocalDate} in the future. By default, 3000-01-01.
	 */
	@SuppressWarnings("checkstyle:MagicNumber")
	public LocalDate getLocalDateInFuture()
	{
		return LocalDate.of(3000, 1, 1);
	}
}
