package software.xdev.tci.demo.tci.db;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.mariadb.jdbc.MariaDbDataSource;

import software.xdev.tci.db.BaseDBTCI;
import software.xdev.tci.db.persistence.classfinder.DynamicPersistenceClassFinder;
import software.xdev.tci.db.persistence.hibernate.HibernateEntityManagerControllerFactory;
import software.xdev.tci.demo.persistence.FlywayInfo;
import software.xdev.tci.demo.persistence.FlywayMigration;
import software.xdev.tci.demo.persistence.config.DefaultJPAConfig;
import software.xdev.tci.demo.tci.db.containers.DBContainer;


public class DBTCI extends BaseDBTCI<DBContainer>
{
	public static final String DB_DATABASE = "test";
	public static final String DB_USERNAME = "test";
	@SuppressWarnings("java:S2068") // This is a test calm down
	public static final String DB_PASSWORD = "test";
	
	private static final DynamicPersistenceClassFinder ENTITY_CLASSES_FINDER = new DynamicPersistenceClassFinder()
		.withSearchForPersistenceClasses(DefaultJPAConfig.ENTITY_PACKAGE);
	
	private static final EntityManagerControllerFactoryProvider EMCF_PROVIDER =
		ServiceLoader.load(EntityManagerControllerFactoryProvider.class)
			.findFirst()
			.orElseGet(() -> HibernateEntityManagerControllerFactory::new);
	
	public DBTCI(
		final DBContainer container,
		final String networkAlias,
		final boolean migrateAndInitializeEMC)
	{
		super(
			container,
			networkAlias,
			migrateAndInitializeEMC,
			() -> EMCF_PROVIDER.create(ENTITY_CLASSES_FINDER));
		this.withDatabase(DB_DATABASE)
			.withUsername(DB_USERNAME)
			.withPassword(DB_PASSWORD);
	}
	
	@Override
	protected void execInitialDatabaseMigration()
	{
		this.migrateDatabase(FlywayInfo.FLYWAY_LOOKUP_STRUCTURE);
	}
	
	public static String getInternalJDBCUrl(final String networkAlias)
	{
		return "jdbc:mariadb://" + networkAlias + ":" + DBContainer.PORT + "/" + DB_DATABASE;
	}
	
	@Override
	protected Class<? extends Driver> driverClazz()
	{
		return org.mariadb.jdbc.Driver.class;
	}
	
	@Override
	@SuppressWarnings("java:S6437") // This is a test calm down
	public DataSource createDataSource()
	{
		final MariaDbDataSource dataSource = new MariaDbDataSource();
		try
		{
			dataSource.setUser(this.username);
			dataSource.setPassword(this.password);
			dataSource.setUrl(this.getExternalJDBCUrl());
		}
		catch(final SQLException e)
		{
			throw new IllegalStateException("Invalid container setup", e);
		}
		return dataSource;
	}
	
	@Override
	public void migrateDatabase(final String... locations)
	{
		new FlywayMigration().migrate(conf ->
		{
			conf.dataSource(this.createDataSource());
			conf.locations(locations);
		});
	}
}
