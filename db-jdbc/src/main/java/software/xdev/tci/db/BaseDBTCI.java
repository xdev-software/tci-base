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
package software.xdev.tci.db;

import java.sql.Driver;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import software.xdev.tci.TCI;
import software.xdev.tci.db.persistence.EntityManagerController;
import software.xdev.tci.db.persistence.EntityManagerControllerFactory;


public abstract class BaseDBTCI<C extends JdbcDatabaseContainer<C>> extends TCI<C>
{
	protected static final Map<Class<?>, Logger> LOGGER_CACHE = new ConcurrentHashMap<>();
	
	public static final String DEFAULT_DATABASE = "testdb";
	public static final String DEFAULT_USERNAME = "testuser";
	@SuppressWarnings("java:S2068") // This is only for tests
	public static final String DEFAULT_PASSWORD = "testpw";
	
	protected final boolean migrateAndInitializeEMC;
	protected final Supplier<EntityManagerControllerFactory<?, ?>> emcFactorySupplier;
	protected final Logger logger;
	
	protected String database = DEFAULT_DATABASE;
	protected String username = DEFAULT_USERNAME;
	@SuppressWarnings("java:S2068") // This is only for tests
	protected String password = DEFAULT_PASSWORD;
	
	protected EntityManagerController emc;
	
	protected BaseDBTCI(
		final C container,
		final String networkAlias,
		final boolean migrateAndInitializeEMC,
		final Supplier<EntityManagerControllerFactory<?, ?>> emcFactorySupplier)
	{
		super(container, networkAlias);
		this.migrateAndInitializeEMC = migrateAndInitializeEMC;
		this.emcFactorySupplier = emcFactorySupplier;
		
		this.logger = LOGGER_CACHE.computeIfAbsent(this.getClass(), LoggerFactory::getLogger);
	}
	
	@Override
	public void start(final String containerName)
	{
		super.start(containerName);
		if(this.migrateAndInitializeEMC)
		{
			// Do basic migrations async
			this.log().debug("Running migration to basic structure");
			this.execInitialDatabaseMigration();
			this.log().info("Migration executed");
			
			// Create EMC in background to improve performance (~5s)
			this.log().debug("Initializing EntityManagerController...");
			this.getEMC();
			this.log().info("Initialized EntityManagerController");
		}
	}
	
	@Override
	public void stop()
	{
		if(this.emc != null)
		{
			try
			{
				this.emc.close();
			}
			catch(final Exception ex)
			{
				this.log().warn("Failed to close EntityManagerController", ex);
			}
			this.emc = null;
		}
		super.stop();
	}
	
	public EntityManagerController getEMC()
	{
		if(this.emc == null)
		{
			this.initEMCIfRequired();
		}
		
		return this.emc;
	}
	
	protected abstract Class<? extends Driver> driverClazz();
	
	protected synchronized void initEMCIfRequired()
	{
		if(this.emc != null)
		{
			return;
		}
		
		this.emc = this.createEMC(this.emcFactorySupplier.get());
	}
	
	protected EntityManagerController createEMC(final EntityManagerControllerFactory<?, ?> factory)
	{
		return factory
			.withDriverFullClassName(this.driverClazz().getName())
			.withJdbcUrl(this.getExternalJDBCUrl())
			.withUsername(this.username)
			.withPassword(this.password)
			.build();
	}
	
	public String getExternalJDBCUrl()
	{
		return this.getContainer().getJdbcUrl();
	}
	
	/**
	 * Creates a new {@link EntityManager} with an internal {@link EntityManagerFactory}, which can be used to load and
	 * save data in the database for the test.
	 *
	 * <p>
	 * It may be a good idea to close the EntityManager, when you're finished with it.
	 * </p>
	 * <p>
	 * All created EntityManager are automatically cleaned up when the test is finished.
	 * </p>
	 *
	 * @return EntityManager
	 */
	public EntityManager createEntityManager()
	{
		return this.getEMC().createEntityManager();
	}
	
	public void useNewEntityManager(final Consumer<EntityManager> action)
	{
		try(final EntityManager em = this.createEntityManager())
		{
			action.accept(em);
		}
	}
	
	@SuppressWarnings("java:S6437") // Only done for test
	public abstract DataSource createDataSource();
	
	protected abstract void execInitialDatabaseMigration();
	
	public void migrateDatabase(final Collection<String> locations)
	{
		this.migrateDatabase(locations.toArray(String[]::new));
	}
	
	public abstract void migrateDatabase(final String... locations);
	
	protected Logger log()
	{
		return this.logger;
	}
	
	// region Configure
	
	public BaseDBTCI<C> withDatabase(final String database)
	{
		this.database = database;
		return this;
	}
	
	public BaseDBTCI<C> withUsername(final String username)
	{
		this.username = username;
		return this;
	}
	
	public BaseDBTCI<C> withPassword(final String password)
	{
		this.password = password;
		return this;
	}
	
	public boolean isMigrateAndInitializeEMC()
	{
		return this.migrateAndInitializeEMC;
	}
	
	// endregion
}
