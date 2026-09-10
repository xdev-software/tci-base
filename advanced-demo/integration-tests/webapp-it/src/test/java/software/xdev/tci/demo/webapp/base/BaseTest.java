package software.xdev.tci.demo.webapp.base;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import software.xdev.tci.TCI;
import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.demo.tci.db.DBTCI;
import software.xdev.tci.demo.tci.db.factory.DBTCIFactory;
import software.xdev.tci.demo.tci.webapp.WebAppTCI;
import software.xdev.tci.demo.tci.webapp.factory.WebAppTCIFactory;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.jacoco.testbase.JaCoCoRecorder;
import software.xdev.tci.junit.jupiter.FileSystemFriendlyName;
import software.xdev.tci.network.LazyNetworkPool;
import software.xdev.tci.oidc.OIDCTCI;
import software.xdev.tci.oidc.factory.OIDCTCIFactory;
import software.xdev.tci.selenium.BrowserTCI;
import software.xdev.tci.selenium.TestBrowser;
import software.xdev.tci.selenium.factory.BrowsersTCIFactory;
import software.xdev.tci.selenium.testbase.SeleniumRecorder;
import software.xdev.tci.tracing.TCITracer;


@ExtendWith(BaseTest.TCSTSeleniumIntegrationTestExtension.class)
public abstract class BaseTest implements IntegrationTestDefaults<BaseTest>
{
	private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);
	
	private static final TCITracer.Timed TRACE_START_BASE_INFRA = new TCITracer.Timed();
	private static final TCITracer.Timed TRACE_START_WEB_DRIVER = new TCITracer.Timed();
	
	private Network network;
	
	protected static final String DNS_NAME_DB = "db";
	protected static final String DNS_NAME_OIDC = "oidc";
	protected static final String DNS_NAME_WEBAPP = "webapp";
	
	protected static final DBTCIFactory DB_INFRA_FACTORY = new DBTCIFactory();
	protected static final OIDCTCIFactory OIDC_INFRA_FACTORY = new OIDCTCIFactory();
	protected static final WebAppTCIFactory APP_INFRA_FACTORY =
		new WebAppTCIFactory(c -> c.withDB(
				DBTCI.getInternalJDBCUrl(DNS_NAME_DB),
				DBTCI.DB_USERNAME,
				DBTCI.DB_PASSWORD
			)
			.withAuth(
				OIDCTCI.CLIENT_ID,
				OIDCTCI.CLIENT_SECRET,
				OIDCTCI.getInternalHttpBaseEndPoint(DNS_NAME_OIDC)
			));
	protected static final BrowsersTCIFactory<TestBrowser> BROWSER_INFRA_FACTORY = BrowsersTCIFactory.createDefault();
	protected static final LazyNetworkPool LAZY_NETWORK_POOL = new LazyNetworkPool();
	
	private DBTCI dbInfra;
	private OIDCTCI oidcInfra;
	private WebAppTCI appInfra;
	private BrowserTCI browserInfra;
	
	private RemoteWebDriver remoteWebDriver;
	
	@BeforeAll
	public static void setup()
	{
		LAZY_NETWORK_POOL.managePoolAsync();
		
		TCIFactoryRegistry.instance().warmUp();
	}
	
	public void startAll(final TestBrowser testBrowser)
	{
		this.startAll(testBrowser, null);
	}
	
	public void startAll(final TestBrowser testBrowser, final Consumer<DBTCI> onDataBaseMigrated)
	{
		this.startBaseInfrastructure(onDataBaseMigrated);
		this.startWebDriver(testBrowser);
	}
	
	protected void startBaseInfrastructure(final Consumer<DBTCI> onDataBaseMigrated)
	{
		final long start = System.currentTimeMillis();
		
		CompletableFuture<OIDCTCI> cfOIDC = null;
		CompletableFuture<WebAppTCI> cfApp = null;
		try
		{
			this.network = LAZY_NETWORK_POOL.getNew();
			
			cfOIDC = CompletableFuture.supplyAsync(
				() -> OIDC_INFRA_FACTORY.getNew(this.network, DNS_NAME_OIDC), TCIExecutorServiceHolder.instance());
			
			cfApp = CompletableFuture.supplyAsync(
				() -> APP_INFRA_FACTORY.getNew(this.network, DNS_NAME_WEBAPP), TCIExecutorServiceHolder.instance());
			
			this.dbInfra = DB_INFRA_FACTORY.getNew(this.network, DNS_NAME_DB);
			Optional.ofNullable(onDataBaseMigrated).ifPresent(c -> c.accept(this.dbInfra));
			
			LOG.info(">>> User: {}", DBTCI.DB_USERNAME);
			LOG.info(">>> Password: {}", DBTCI.DB_PASSWORD);
			LOG.info(">>> JDBC (external): {}", this.dbInfra.getExternalJDBCUrl());
			
			this.oidcInfra = cfOIDC.join();
			LOG.info(">>> OIDC Server: {}", this.oidcInfra.getExternalHttpBaseEndPoint());
			
			this.appInfra = cfApp.join();
			LOG.info(">>> HTTP Interface (external): {}", this.appInfra.getExternalHTTPEndpoint());
		}
		catch(final Exception ex)
		{
			// Ensure that we do not leak when e.g. DB migration fails
			this.ensureDestroyAsyncStartingInfra(() -> this.oidcInfra, i -> this.oidcInfra = i, cfOIDC);
			this.ensureDestroyAsyncStartingInfra(() -> this.appInfra, i -> this.appInfra = i, cfApp);
			
			throw new RuntimeException("Failed to setup base infrastructure", ex);
		}
		TRACE_START_BASE_INFRA.addMs(System.currentTimeMillis() - start);
	}
	
	protected <T extends TCI<?>> void ensureDestroyAsyncStartingInfra(
		final Supplier<T> getCurrentInfra,
		final Consumer<T> setCurrentInfra,
		final CompletableFuture<T> cfStartingInfra)
	{
		if(getCurrentInfra.get() != null // Infra was already started and set
			|| cfStartingInfra == null) // Infra was never started
		{
			return;
		}
		try
		{
			// Try to get starting infra and set it, so it can be properly destroyed
			setCurrentInfra.accept(cfStartingInfra.get(3, TimeUnit.MINUTES));
		}
		catch(final InterruptedException iex)
		{
			LOG.warn("Got interrupted", iex);
			Thread.currentThread().interrupt();
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to get starting infra in time", ex);
		}
	}
	
	public void startWebDriver(final TestBrowser testBrowser)
	{
		final long start = System.currentTimeMillis();
		try
		{
			this.browserInfra = BROWSER_INFRA_FACTORY.getNew(testBrowser, this.network);
			this.browserInfra.getVncAddress().ifPresent(a -> LOG.info(">>> VNC: {}", a));
			this.browserInfra.getNoVncAddress().ifPresent(a -> LOG.info(">>> NoVNC: {}", a));
			
			this.remoteWebDriver = this.browserInfra.getWebDriver();
			LOG.debug("Got WebDriver");
		}
		catch(final Exception ex)
		{
			throw new RuntimeException("Failed to setup webDriver", ex);
		}
		TRACE_START_WEB_DRIVER.addMs(System.currentTimeMillis() - start);
	}
	
	protected void stopWebDriver()
	{
		if(this.browserInfra == null)
		{
			return;
		}
		
		final RemoteWebDriver fRemoteWebDriver = this.remoteWebDriver;
		final BrowserTCI fBrowserInfra = this.browserInfra;
		
		CompletableFuture.runAsync(() -> {
			try
			{
				if(fRemoteWebDriver != null && fRemoteWebDriver.getSessionId() != null)
				{
					LOG.info("Quiting remoteWebDriver");
					fRemoteWebDriver.quit();
				}
				
				fBrowserInfra.stop();
			}
			catch(final Exception ex)
			{
				LOG.warn("Failed to stop WebDriver(async)", ex);
			}
		}, TCIExecutorServiceHolder.instance());
		
		this.remoteWebDriver = null;
		this.browserInfra = null;
	}
	
	protected void stopEverything()
	{
		LOG.info("Shutting down");
		
		final WebAppTCI fAppInfra = this.appInfra;
		final OIDCTCI fOidcInfra = this.oidcInfra;
		final DBTCI fDbInfra = this.dbInfra;
		
		final Network fNetwork = this.network;
		
		CompletableFuture.runAsync(() -> {
			try
			{
				Stream.<Runnable>concat(
						Stream.of(this::stopWebDriver),
						Stream.of(fAppInfra, fOidcInfra, fDbInfra)
							.filter(Objects::nonNull)
							.map(tci -> tci::stop))
					.map(CompletableFuture::runAsync)
					.toList() // collect so everything is getting executed async
					.forEach(CompletableFuture::join);
				
				Optional.ofNullable(fNetwork).ifPresent(Network::close);
			}
			catch(final Exception ex)
			{
				LOG.error("Failed to stop everything(async)", ex);
			}
		}, TCIExecutorServiceHolder.instance());
		
		this.appInfra = null;
		this.oidcInfra = null;
		this.dbInfra = null;
		
		this.network = null;
	}
	
	public WebDriver getWebDriver()
	{
		return this.remoteWebDriver;
	}
	
	public Network network()
	{
		return this.network;
	}
	
	public DBTCI dbInfra()
	{
		return this.dbInfra;
	}
	
	public OIDCTCI oidcInfra()
	{
		return this.oidcInfra;
	}
	
	public WebAppTCI appInfra()
	{
		return this.appInfra;
	}
	
	public BrowserTCI browserInfra()
	{
		return this.browserInfra;
	}
	
	public String getWebAppBaseUrl()
	{
		return this.appInfra.getInternalHTTPEndpoint();
	}
	
	public static class TCSTSeleniumIntegrationTestExtension
		implements BeforeTestExecutionCallback, AfterTestExecutionCallback
	{
		private static final Logger LOG = LoggerFactory.getLogger(TCSTSeleniumIntegrationTestExtension.class);
		
		@Override
		public void beforeTestExecution(final ExtensionContext context)
		{
			LOG.info("vvvvvv--START TEST--vvvvvv");
		}
		
		@Override
		public void afterTestExecution(final ExtensionContext context)
		{
			LOG.info("^^^^^^--END TEST--^^^^^^");
			final Optional<Throwable> executionExceptionOpt = context.getExecutionException();
			executionExceptionOpt.ifPresent(throwable -> LOG.error("Test-Failure", throwable));
			
			final FileSystemFriendlyName fileSystemFriendlyName = new FileSystemFriendlyName(context);
			
			CompletableFuture.allOf(
				new SeleniumRecorder().afterTestAsync(
					context,
					context.getTestInstance()
						.filter(BaseTest.class::isInstance)
						.map(BaseTest.class::cast)
						.map(BaseTest::browserInfra),
					fileSystemFriendlyName),
				JaCoCoRecorder.instance().afterTestAsync(
					context.getTestInstance()
						.filter(BaseTest.class::isInstance)
						.map(BaseTest.class::cast)
						.map(BaseTest::appInfra),
					fileSystemFriendlyName)
			).join();
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
					BaseInfra: {}
					WebDriver: {}""",
				TRACE_START_BASE_INFRA,
				TRACE_START_WEB_DRIVER);
		}
	}
}
