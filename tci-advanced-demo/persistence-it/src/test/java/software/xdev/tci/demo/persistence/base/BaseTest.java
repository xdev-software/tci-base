package software.xdev.tci.demo.persistence.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.demo.persistence.FlywayInfo;
import software.xdev.tci.demo.tci.db.DBTCI;
import software.xdev.tci.demo.tci.db.factory.DBTCIFactory;
import software.xdev.tci.demo.tci.util.ContainerLoggingUtil;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.leakdetection.LeakDetectionAsyncReaper;
import software.xdev.tci.tracing.TCITracer;


@ExtendWith(BaseTest.LogExtension.class)
public abstract class BaseTest
{
	private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);
	
	static final Set<CompletableFuture<?>> REAP_CFS =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	
	static final TCITracer.Timed TRACE_START_INFRA = new TCITracer.Timed();
	static final TCITracer.Timed TRACE_START_INFRA_MIGRATE_DB = new TCITracer.Timed();
	static final TCITracer.Timed TRACE_START_INFRA_CHECK_EMC = new TCITracer.Timed();
	
	static final DAOInjector DAO_INJECTOR = new DAOInjector();
	
	static final DBTCIFactory DB_INFRA_FACTORY = new DBTCIFactory(false);
	
	private DBTCI dbInfra;
	
	@BeforeAll
	public static void setup()
	{
		ContainerLoggingUtil.redirectJULtoSLF4J();
		
		TCIFactoryRegistry.instance().warmUp();
	}
	
	protected void startInfra(final String... additionalMigrationLocations)
	{
		this.startInfra(true, additionalMigrationLocations);
	}
	
	protected void startInfra(
		final boolean withDefaultData,
		final String... additionalMigrationLocations)
	{
		this.startInfra(withDefaultData, List.of(additionalMigrationLocations));
	}
	
	protected void startInfra(
		final boolean withDefaultData,
		final Collection<String> additionalMigrationLocations)
	{
		final List<String> migrationLocations = new ArrayList<>(List.of(FlywayInfo.FLYWAY_LOOKUP_STRUCTURE));
		if(withDefaultData)
		{
			migrationLocations.add("init/extended");
		}
		migrationLocations.addAll(additionalMigrationLocations);
		
		this.startInfra(migrationLocations);
	}
	
	protected void startInfra(final Collection<String> migrationLocations)
	{
		final long startTime = System.currentTimeMillis();
		try
		{
			this.dbInfra = DB_INFRA_FACTORY.getNew(null);
			
			LOG.info(">>>> User: {}", DBTCI.DB_USERNAME);
			LOG.info(">>>> Password: {}", DBTCI.DB_PASSWORD);
			LOG.info(">>>> JDBC: {}", this.dbInfra.getExternalJDBCUrl());
			
			final long startTimeMigrate = System.currentTimeMillis();
			this.dbInfra.migrateDatabase(migrationLocations);
			TRACE_START_INFRA_MIGRATE_DB.addMs(System.currentTimeMillis() - startTimeMigrate);
			
			final long startTimeEMC = System.currentTimeMillis();
			Unreliables.retryUntilSuccess(
				10,
				TimeUnit.SECONDS,
				() -> {
					this.dbInfra.useNewEntityManager(em -> em.createNativeQuery("SELECT 1").getResultList());
					return null;
				});
			TRACE_START_INFRA_CHECK_EMC.addMs(System.currentTimeMillis() - startTimeEMC);
			
			DAO_INJECTOR.doInjections(this.getClass(), this.dbInfra::createEntityManager, () -> this);
			
			TRACE_START_INFRA.addMs(System.currentTimeMillis() - startTime);
		}
		catch(final RuntimeException ex)
		{
			LOG.error("Failed to start infra", ex);
			throw ex;
		}
	}
	
	protected void stopInfra()
	{
		if(this.dbInfra != null)
		{
			this.dbInfra.logDataBaseInfo();
			
			final DBTCI dbInfra = this.dbInfra;
			REAP_CFS.add(CompletableFuture.runAsync(dbInfra::stop));
			
			this.dbInfra = null;
		}
	}
	
	public static class LogExtension implements BeforeEachCallback, AfterEachCallback
	{
		private static final Logger LOG = LoggerFactory.getLogger(LogExtension.class);
		
		@Override
		public void afterEach(final ExtensionContext context)
		{
			LOG.info("Running test (displayname): {}", context.getDisplayName());
			LOG.info("vvvvvv--START TEST--vvvvvv");
		}
		
		@Override
		public void beforeEach(final ExtensionContext context)
		{
			LOG.info("^^^^^^--END TEST--^^^^^^");
			context.getExecutionException().ifPresent(throwable -> LOG.error("Test-Failure", throwable));
		}
	}
	
	
	public static class Tracer implements TestExecutionListener
	{
		private static final Logger LOG = LoggerFactory.getLogger(Tracer.class);
		
		@Override
		public void testPlanExecutionFinished(final TestPlan testPlan)
		{
			LOG.info(
				"""
					=== TRACER ===
					Start-Infra: {}
					 - Migrate DB: {}
					 - Check EMC: {}""",
				TRACE_START_INFRA,
				TRACE_START_INFRA_MIGRATE_DB,
				TRACE_START_INFRA_CHECK_EMC);
		}
	}
	
	
	public static class Reaper implements LeakDetectionAsyncReaper
	{
		@Override
		public void blockUntilReaped()
		{
			REAP_CFS.stream()
				.filter(Objects::nonNull)
				.forEach(CompletableFuture::join);
		}
	}
}
