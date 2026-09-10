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

import static java.util.Map.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.PersistenceUnitInfo;


@SuppressWarnings("java:S119")
public abstract class EntityManagerControllerFactory<
	SELF extends EntityManagerControllerFactory<SELF, P>,
	P extends PersistenceUnitInfo>
{
	protected Supplier<Set<String>> entityClassesFinder;
	protected String driverFullClassName;
	protected String persistenceUnitName = "Test";
	protected String jdbcUrl;
	protected String username;
	protected String password;
	protected Map<String, Object> additionalConfig;
	
	protected EntityManagerControllerFactory()
	{
	}
	
	protected EntityManagerControllerFactory(final Supplier<Set<String>> entityClassesFinder)
	{
		this.withEntityClassesFinder(entityClassesFinder);
	}
	
	public SELF withEntityClassesFinder(final Supplier<Set<String>> entityClassesFinder)
	{
		this.entityClassesFinder = entityClassesFinder;
		return this.self();
	}
	
	public SELF withDriverFullClassName(final String driverFullClassName)
	{
		this.driverFullClassName = driverFullClassName;
		return this.self();
	}
	
	public SELF withPersistenceUnitName(final String persistenceUnitName)
	{
		this.persistenceUnitName = persistenceUnitName;
		return this.self();
	}
	
	public SELF withJdbcUrl(final String jdbcUrl)
	{
		this.jdbcUrl = jdbcUrl;
		return this.self();
	}
	
	public SELF withUsername(final String username)
	{
		this.username = username;
		return this.self();
	}
	
	public SELF withPassword(final String password)
	{
		this.password = password;
		return this.self();
	}
	
	public SELF withAdditionalConfig(final Map<String, Object> additionalConfig)
	{
		this.additionalConfig = additionalConfig;
		return this.self();
	}
	
	protected abstract P createPersistenceUnitInfo();
	
	protected Map<String, Object> defaultPropertiesMap()
	{
		return new HashMap<>(Map.ofEntries(
			entry(PersistenceConfiguration.JDBC_DRIVER, this.driverFullClassName),
			entry(PersistenceConfiguration.JDBC_URL, this.jdbcUrl),
			entry(PersistenceConfiguration.JDBC_USER, this.username),
			entry(PersistenceConfiguration.JDBC_PASSWORD, this.password)
		));
	}
	
	public EntityManagerController build()
	{
		final PersistenceUnitInfo persistenceUnitInfo = this.createPersistenceUnitInfo();
		if(this.entityClassesFinder != null)
		{
			persistenceUnitInfo.getManagedClassNames().addAll(this.entityClassesFinder.get());
		}
		
		return this.createEntityManagerController(this.createEntityManagerFactory(
			persistenceUnitInfo,
			this.buildProperties()));
	}
	
	protected EntityManagerController createEntityManagerController(final EntityManagerFactory factory)
	{
		return new EntityManagerController(factory);
	}
	
	protected Map<String, Object> buildProperties()
	{
		final Map<String, Object> properties = this.defaultPropertiesMap();
		if(this.additionalConfig != null)
		{
			properties.putAll(this.additionalConfig);
		}
		return properties;
	}
	
	protected abstract EntityManagerFactory createEntityManagerFactory(
		final PersistenceUnitInfo pui,
		final Map<String, Object> properties);
	
	@SuppressWarnings("unchecked")
	protected SELF self()
	{
		return (SELF)this;
	}
}
