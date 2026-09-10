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
package software.xdev.tci.db.persistence.eclipselink;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.springframework.orm.jpa.persistenceunit.SpringPersistenceUnitInfo;

import software.xdev.tci.db.persistence.SpringEntityManagerControllerFactory;


public class EclipseLinkEntityManagerControllerFactory
	extends SpringEntityManagerControllerFactory<PersistenceProvider, EclipseLinkEntityManagerControllerFactory>
{
	// Disabled (false) by default as it's not used by the default created SpringPersistenceUnitInfo anyway
	protected boolean weavingEnabled;
	protected ClassLoader persistenceUnitInfoCl = this.getClass().getClassLoader();
	
	public EclipseLinkEntityManagerControllerFactory()
	{
	}
	
	public EclipseLinkEntityManagerControllerFactory(final Supplier<Set<String>> entityClassesFinder)
	{
		super(entityClassesFinder);
	}
	
	public EclipseLinkEntityManagerControllerFactory withWeavingEnabled(final boolean weavingEnabled)
	{
		this.weavingEnabled = weavingEnabled;
		return this;
	}
	
	public EclipseLinkEntityManagerControllerFactory withPersistenceUnitInfoCl(final ClassLoader persistenceUnitInfoCl)
	{
		this.persistenceUnitInfoCl = persistenceUnitInfoCl;
		return this;
	}
	
	@Override
	protected SpringPersistenceUnitInfo instantiateSpringPersistenceUnitInfo()
	{
		return new SpringPersistenceUnitInfo(this.persistenceUnitInfoCl);
	}
	
	@Override
	protected SpringPersistenceUnitInfo createSpringPersistenceUnitInfo()
	{
		final SpringPersistenceUnitInfo spui = super.createSpringPersistenceUnitInfo();
		// Required otherwise createContainerEntityManagerFactoryImpl crashes
		spui.setPersistenceUnitRootUrl(this.getClass().getResource(""));
		return spui;
	}
	
	@Override
	protected Map<String, Object> buildProperties()
	{
		final Map<String, Object> properties = super.buildProperties();
		if(!this.weavingEnabled)
		{
			properties.put(PersistenceUnitProperties.WEAVING, false);
		}
		return properties;
	}
	
	@Override
	protected PersistenceProvider createDefaultPersistenceProvider()
	{
		return new PersistenceProvider();
	}
}
