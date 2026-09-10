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
package software.xdev.tci.selenium;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.bidi.log.LogLevel;
import org.openqa.selenium.bidi.log.StackTrace;
import org.openqa.selenium.bidi.module.LogInspector;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;
import org.rnorth.ducttape.timeouts.Timeouts;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.lifecycle.TestDescription;

import software.xdev.tci.TCI;
import software.xdev.tci.envperf.EnvironmentPerformance;
import software.xdev.tci.logging.JULtoSLF4JRedirector;
import software.xdev.tci.selenium.containers.SeleniumBrowserWebDriverContainer;


public class BrowserTCI extends TCI<SeleniumBrowserWebDriverContainer>
{
	private static final Logger LOG = LoggerFactory.getLogger(BrowserTCI.class);
	
	protected final MutableCapabilities capabilities;
	
	// Disables the (not standardized) Chrome DevTools protocol (CDP) when bidi is enabled.
	// CDP requires additional maven dependencies (e.g. selenium-devtools-v137) that are
	// NOT present and result in a warning.
	protected boolean deactivateCDPIfPossible = true;
	
	protected ClientConfig clientConfig = ClientConfig.defaultConfig();
	protected int webDriverRetryCount = 2;
	protected int webDriverRetrySec = 30;
	protected Consumer<String> browserConsoleLogConsumer;
	protected Set<LogLevel> browserConsoleLogLevels;
	
	protected RemoteWebDriver webDriver;
	protected LogInspector logInspector;
	
	public BrowserTCI(
		final SeleniumBrowserWebDriverContainer container,
		final String networkAlias,
		final MutableCapabilities capabilities)
	{
		super(container, networkAlias);
		this.capabilities = capabilities;
	}
	
	public BrowserTCI withDeactivateCDPIfPossible(final boolean deactivateCDPIfPossible)
	{
		this.deactivateCDPIfPossible = deactivateCDPIfPossible;
		return this;
	}
	
	public BrowserTCI withClientConfig(final ClientConfig clientConfig)
	{
		this.clientConfig = Objects.requireNonNull(clientConfig);
		return this;
	}
	
	public BrowserTCI withWebDriverRetryCount(final int webDriverRetryCount)
	{
		this.webDriverRetryCount = Math.clamp(webDriverRetryCount, 2, 10);
		return this;
	}
	
	public BrowserTCI withWebDriverRetrySec(final int webDriverRetrySec)
	{
		this.webDriverRetrySec = Math.clamp(webDriverRetrySec, 10, 10 * 60);
		return this;
	}
	
	public BrowserTCI withBrowserConsoleLog(
		final Consumer<String> browserConsoleLogConsumer,
		final Set<LogLevel> browserConsoleLogLevels)
	{
		this.browserConsoleLogConsumer = browserConsoleLogConsumer;
		this.browserConsoleLogLevels = browserConsoleLogLevels;
		return this;
	}
	
	@Override
	public void start(final String containerName)
	{
		super.start(containerName);
		
		// Selenium uses JUL
		JULtoSLF4JRedirector.redirect();
		
		this.initWebDriver();
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	protected void initWebDriver()
	{
		LOG.debug("Initializing WebDriver");
		final AtomicInteger retryCounter = new AtomicInteger(1);
		this.webDriver = Unreliables.retryUntilSuccess(
			this.webDriverRetryCount,
			() -> this.tryCreateWebDriver(retryCounter));
		
		// Default timeout is 5m? -> Single test failure causes up to 10m delays
		// https://w3c.github.io/webdriver/#timeouts
		this.webDriver.manage().timeouts().pageLoadTimeout(
			Duration.ofSeconds(40L + 20L * EnvironmentPerformance.cpuSlownessFactor()));
		
		// Maximize window
		this.webDriver.manage().window().maximize();
		
		this.installBrowserLogInspector();
	}
	
	protected RemoteWebDriver tryCreateWebDriver(final AtomicInteger retryCounter)
	{
		final ClientConfig config =
			this.clientConfig.baseUri(this.getContainer().getSeleniumAddressURI());
		final int tryCount = retryCounter.getAndIncrement();
		LOG.info(
			"Creating new WebDriver [retryCount={},retrySec={},clientConfig={}] Try #{}",
			this.webDriverRetryCount,
			this.webDriverRetrySec,
			config,
			tryCount);
		
		final HttpClient.Factory factory = HttpCommandExecutor.getDefaultClientFactory();
		@SuppressWarnings("java:S2095") // Handled by Selenium when quit is called
		final HttpClient client = factory.createClient(config);
		
		final HttpCommandExecutor commandExecutor = this.createCommandExecutor(config, client);
		
		try
		{
			return Timeouts.getWithTimeout(
				this.webDriverRetrySec,
				TimeUnit.SECONDS,
				() -> {
					this.capabilities.setCapability("webSocketUrl", true);
					if(this.deactivateCDPIfPossible)
					{
						this.modifyCapsDisableCDP();
					}
					
					return new RemoteWebDriver(commandExecutor, this.capabilities, config);
				});
		}
		catch(final RuntimeException rex)
		{
			// Cancel further communication and abort all connections
			try
			{
				LOG.warn("Encounter problem in try #{} - Terminating communication", tryCount, rex);
				client.close();
				factory.cleanupIdleClients();
			}
			catch(final Exception ex)
			{
				LOG.warn("Failed to cleanup try #{}", tryCount, ex);
			}
			
			throw rex;
		}
	}
	
	protected HttpCommandExecutor createCommandExecutor(final ClientConfig config, final HttpClient client)
	{
		return new HttpCommandExecutor(
			Map.of(),
			config,
			// Constructor without factory does not exist...
			ignored -> client);
	}
	
	protected void modifyCapsDisableCDP()
	{
		this.capabilities.setCapability("se:cdpEnabled", Boolean.FALSE.toString());
	}
	
	protected void installBrowserLogInspector()
	{
		if(this.browserConsoleLogConsumer == null)
		{
			return;
		}
		
		LOG.info("Installing Log Inspector");
		
		this.logInspector = new LogInspector(this.webDriver);
		final String name = this.getContainer().getContainerNameCleaned();
		this.logInspector.onConsoleEntry(c -> {
			if(this.browserConsoleLogLevels == null || this.browserConsoleLogLevels.contains(c.getLevel()))
			{
				this.browserConsoleLogConsumer.accept(
					"[" + name + "] "
						+ c.getLevel().name().toUpperCase() + " "
						+ c.getText()
						+ Optional.ofNullable(c.getStackTrace()) // Note: 2024-11 Stacktrace is not present in FF
						.map(StackTrace::getCallFrames)
						.map(cf -> "\n" + cf.stream()
							.map(s -> "-> "
								+ s.getFunctionName()
								+ "@" + s.getUrl()
								+ " L" + s.getLineNumber()
								+ "C" + s.getColumnNumber()).collect(
								Collectors.joining("\n")))
						.orElse(""));
			}
		});
	}
	
	public Optional<String> getVncAddress()
	{
		return Optional.ofNullable(this.getContainer().getVncAddress());
	}
	
	public Optional<String> getNoVncAddress()
	{
		return Optional.ofNullable(this.getContainer().getNoVncAddress());
	}
	
	public RemoteWebDriver getWebDriver()
	{
		return this.webDriver;
	}
	
	public void afterTest(final TestDescription description, final Optional<Throwable> throwable)
	{
		if(this.getContainer() != null)
		{
			this.getContainer().afterTest(description, throwable);
		}
	}
	
	@Override
	public void stop()
	{
		if(this.logInspector != null)
		{
			final long startMs = System.currentTimeMillis();
			try
			{
				this.logInspector.close();
			}
			catch(final Exception e)
			{
				LOG.warn("Failed to stop logInspector", e);
			}
			finally
			{
				LOG.debug("Stopping logInspector took {}ms", System.currentTimeMillis() - startMs);
			}
			this.logInspector = null;
		}
		if(this.webDriver != null)
		{
			final long startMs = System.currentTimeMillis();
			try
			{
				this.webDriver.quit();
			}
			catch(final Exception e)
			{
				LOG.warn("Failed to quit webdriver", e);
			}
			finally
			{
				LOG.debug("Quiting driver took {}ms", System.currentTimeMillis() - startMs);
			}
			this.webDriver = null;
		}
		super.stop();
	}
}
