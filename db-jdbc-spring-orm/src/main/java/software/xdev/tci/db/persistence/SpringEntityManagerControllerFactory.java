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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.springframework.orm.jpa.persistenceunit.SpringPersistenceUnitInfo;


@SuppressWarnings("java:S119")
public abstract class SpringEntityManagerControllerFactory<
	P extends PersistenceProvider,
	SELF extends SpringEntityManagerControllerFactory<P, SELF>>
	extends EntityManagerControllerFactory<SELF, PersistenceUnitInfo>
{
	protected boolean addJarFileUrls;
	protected P persistenceProvider;
	
	protected SpringEntityManagerControllerFactory()
	{
	}
	
	protected SpringEntityManagerControllerFactory(final Supplier<Set<String>> entityClassesFinder)
	{
		super(entityClassesFinder);
	}
	
	/**
	 * Should JAR files be added to scan for Entities, Converters, etc.?
	 * <p>
	 * This might impact performance because by default all JARs are scanned.
	 * </p>
	 * <p>
	 * If possible please use the more performant
	 * {@link SpringEntityManagerControllerFactory#SpringEntityManagerControllerFactory(Supplier)} instead.
	 * </p>
	 */
	public SELF withAddJarFileUrls(final boolean addJarFileUrls)
	{
		this.addJarFileUrls = addJarFileUrls;
		return this.self();
	}
	
	/**
	 * Allows to manually configure a (default) persistence provider
	 */
	public SELF withPersistenceProvider(final P persistenceProvider)
	{
		this.persistenceProvider = persistenceProvider;
		return this.self();
	}
	
	@SuppressWarnings("java:S4449") // can't add annotations to source code that we do not control
	protected SpringPersistenceUnitInfo instantiateSpringPersistenceUnitInfo()
	{
		// This is the default implementation for Spring Boot + Hibernate
		// It might not work with other persistence frameworks, e.g. EclipseLink
		
		// ClassLoader is never needed by Hibernate
		// noinspection DataFlowIssue - ClassLoader can be null! Look at the init method
		return new SpringPersistenceUnitInfo((ClassLoader)null)
		{
			@Override
			public void addTransformer(final ClassTransformer ignored)
			{
				// Do nothing
			}
			
			@Override
			public ClassLoader getNewTempClassLoader()
			{
				// Never needed
				return null;
			}
		};
	}
	
	protected SpringPersistenceUnitInfo createSpringPersistenceUnitInfo()
	{
		final SpringPersistenceUnitInfo pui = this.instantiateSpringPersistenceUnitInfo();
		pui.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
		pui.setPersistenceUnitName(this.persistenceUnitName);
		
		if(this.addJarFileUrls)
		{
			this.jarFileUrlsToAdd().forEach(pui::addJarFileUrl);
		}
		
		return pui;
	}
	
	protected Collection<URL> jarFileUrlsToAdd()
	{
		try
		{
			return Collections.list(EntityManagerController.class
				.getClassLoader()
				.getResources(""));
		}
		catch(final IOException ioe)
		{
			throw new UncheckedIOException(ioe);
		}
	}
	
	@Override
	protected PersistenceUnitInfo createPersistenceUnitInfo()
	{
		return this.createSpringPersistenceUnitInfo().asStandardPersistenceUnitInfo();
	}
	
	protected abstract P createDefaultPersistenceProvider();
	
	@Override
	protected EntityManagerFactory createEntityManagerFactory(
		final PersistenceUnitInfo pui,
		final Map<String, Object> properties)
	{
		if(this.persistenceProvider == null)
		{
			this.persistenceProvider = this.createDefaultPersistenceProvider();
		}
		return this.persistenceProvider.createContainerEntityManagerFactory(pui, properties);
	}
}
