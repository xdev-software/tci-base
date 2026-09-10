/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
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
package software.xdev.tci.leakdetection;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.TCIFactory;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.leakdetection.config.LeakDetectionConfig;
import software.xdev.tci.serviceloading.TCIServiceLoaderHolder;


/**
 * Detects infrastructure that was not disposed properly after all test have ended.
 * <p>
 * If infrastructure is disposed asynchronously, {@link LeakDetectionAsyncReaper} may need to be used.
 * </p>
 * <p>
 * Active by default due to service loading.
 * </p>
 */
public class TCILeakAgent implements TestExecutionListener
{
	private static final Logger LOG = LoggerFactory.getLogger(TCILeakAgent.class);
	
	protected LeakDetectionConfig config;
	
	@Override
	public void testPlanExecutionStarted(final TestPlan testPlan)
	{
		this.config = TCIServiceLoaderHolder.instance().service(LeakDetectionConfig.class);
		if(!this.config.enabled())
		{
			return;
		}
		
		LOG.debug("Registered");
	}
	
	@Override
	public void testPlanExecutionFinished(final TestPlan testPlan)
	{
		if(!this.config.enabled())
		{
			return;
		}
		
		final List<LeakDetectionAsyncReaper> pendingReapers = ServiceLoader.load(LeakDetectionAsyncReaper.class)
			.stream()
			.map(ServiceLoader.Provider::get)
			.toList();
		if(!pendingReapers.isEmpty())
		{
			LOG.info("Waiting for reapers to finish...");
			pendingReapers.forEach(LeakDetectionAsyncReaper::blockUntilReaped);
		}
		
		if(this.waitForAllInfraToFullyStop(this.config.defaultStopTimeout()))
		{
			LOG.info("No leaks detected");
			return;
		}
		
		final Map<TCIFactory<?, ?>, Map<TCI<?>, CompletableFuture<Boolean>>> leaked =
			TCIFactoryRegistry.instance().getReturnedAndInUse();
		if(leaked.isEmpty())
		{
			LOG.info("No leaks detected");
			return;
		}
		
		this.reportLeak(leaked);
	}
	
	/**
	 * @return <code>true</code> if all infra was determined to be stopped
	 */
	protected boolean waitForAllInfraToFullyStop(final Duration defaultStopTimeout)
	{
		if(defaultStopTimeout == null)
		{
			return false;
		}
		
		final Map<TCIFactory<?, ?>, Map<TCI<?>, CompletableFuture<Boolean>>> leaked =
			TCIFactoryRegistry.instance().getReturnedAndInUse();
		if(leaked.isEmpty())
		{
			return true;
		}
		
		try
		{
			final List<CompletableFuture<Boolean>> stopCfs = leaked.values()
				.stream()
				.map(Map::values)
				.flatMap(Collection::stream)
				.toList();
			LOG.info("Waiting for {}x infras to fully stop within {}", stopCfs.size(), defaultStopTimeout);
			
			final long startMs = System.currentTimeMillis();
			CompletableFuture.allOf(stopCfs.toArray(CompletableFuture[]::new))
				.get(defaultStopTimeout.toMillis(), TimeUnit.MILLISECONDS);
			
			LOG.info("Took {}ms to wait until all infras are fully stopped", System.currentTimeMillis() - startMs);
			return true;
		}
		catch(final InterruptedException e)
		{
			LOG.warn("Got interrupted", e);
			Thread.currentThread().interrupt();
		}
		catch(final ExecutionException ignored)
		{
			// Can never happen because the completable future is always successfully completed
		}
		catch(final TimeoutException e)
		{
			LOG.warn("Timed out while waiting for infra to stop[duration={}]", defaultStopTimeout, e);
		}
		return false;
	}
	
	@SuppressWarnings({"java:S2629", "java:S106", "PMD.SystemPrintln"})
	protected void reportLeak(final Map<TCIFactory<?, ?>, Map<TCI<?>, CompletableFuture<Boolean>>> leaked)
	{
		final String baseErrorMsg = "PANIC: DETECTED CONTAINER INFRASTRUCTURE LEAK";
		final String logErrorMsg = "! " + baseErrorMsg + " !";
		final String border = "!".repeat(logErrorMsg.length());
		// Ensure that developer notices PANIC
		System.err.println(border);
		System.err.println(logErrorMsg);
		System.err.println(border);
		
		LOG.error(border);
		LOG.error(logErrorMsg);
		LOG.error(border);
		LOG.error(
			"All test are finished but some infrastructure is still marked as in use:\n{}",
			leaked.entrySet().stream()
				.map(e -> e.getKey().getClass().getSimpleName() + " leaked " + e.getValue().size() + "x "
					+ "[container-ids="
					+ e.getValue().keySet().stream()
					.map(TCI::getContainer)
					.filter(Objects::nonNull)
					.map(Container::getContainerId)
					.toList()
					+ "]")
				.collect(Collectors.joining("\n"))
		);
		LOG.error("Please ensure that every TCI/TestContainerInfrastructure is closed after it's no longer in use");
	}
}
