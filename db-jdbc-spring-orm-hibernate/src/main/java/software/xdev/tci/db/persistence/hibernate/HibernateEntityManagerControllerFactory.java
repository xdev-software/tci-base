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
package software.xdev.tci.db.persistence.hibernate;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.jpa.HibernatePersistenceProvider;

import software.xdev.tci.db.persistence.SpringEntityManagerControllerFactory;


public class HibernateEntityManagerControllerFactory
	extends SpringEntityManagerControllerFactory<HibernatePersistenceProvider, HibernateEntityManagerControllerFactory>
{
	protected String connectionProviderClassName = HikariCPConnectionProvider.class.getName();
	protected boolean disableHibernateFormatter = true;
	protected boolean useCachingStandardScanner = true;
	
	public HibernateEntityManagerControllerFactory()
	{
	}
	
	public HibernateEntityManagerControllerFactory(final Supplier<Set<String>> entityClassesFinder)
	{
		super(entityClassesFinder);
	}
	
	public HibernateEntityManagerControllerFactory withConnectionProviderClassName(
		final String connectionProviderClassName)
	{
		this.connectionProviderClassName = connectionProviderClassName;
		return this;
	}
	
	public HibernateEntityManagerControllerFactory withDisableHibernateFormatter(
		final boolean disableHibernateFormatter)
	{
		this.disableHibernateFormatter = disableHibernateFormatter;
		return this;
	}
	
	public HibernateEntityManagerControllerFactory withUseCachingStandardScanner(
		final boolean useCachingStandardScanner)
	{
		this.useCachingStandardScanner = useCachingStandardScanner;
		return this;
	}
	
	@Override
	protected Map<String, Object> buildProperties()
	{
		final Map<String, Object> properties = super.buildProperties();
		Optional.ofNullable(this.connectionProviderClassName)
			.ifPresent(p -> properties.put(JdbcSettings.CONNECTION_PROVIDER, this.connectionProviderClassName));
		if(this.disableHibernateFormatter)
		{
			properties.putAll(DisableHibernateFormatMapper.properties());
		}
		if(this.useCachingStandardScanner)
		{
			properties.put(PersistenceSettings.SCANNER, CachingStandardScanner.instance());
		}
		return properties;
	}
	
	@Override
	protected HibernatePersistenceProvider createDefaultPersistenceProvider()
	{
		return new HibernatePersistenceProvider();
	}
}
