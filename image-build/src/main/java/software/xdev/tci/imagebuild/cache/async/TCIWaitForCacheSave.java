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
package software.xdev.tci.imagebuild.cache.async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.imagebuild.config.BuildImageHandlerConfig;


public class TCIWaitForCacheSave implements TestExecutionListener
{
	private static final Logger LOG = LoggerFactory.getLogger(TCIWaitForCacheSave.class);
	
	private BuildImageHandlerConfig config;
	
	@Override
	public void testPlanExecutionStarted(final TestPlan testPlan)
	{
		this.config = BuildImageHandlerConfig.instance();
	}
	
	@Override
	public void testPlanExecutionFinished(final TestPlan testPlan)
	{
		if(!this.config.waitForSaveCacheInBackground())
		{
			return;
		}
		
		final List<CompletableFuture<?>> cfs = TCICacheSaveRegistry.instance().get();
		if(cfs.isEmpty())
		{
			return;
		}
		
		LOG.info("Waiting for cache saves to finish...");
		final long startMs = System.currentTimeMillis();
		try
		{
			CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
				.get(10, TimeUnit.MINUTES);
		}
		catch(final InterruptedException e)
		{
			LOG.warn("Got interrupted", e);
			Thread.currentThread().interrupt();
		}
		catch(final ExecutionException e)
		{
			LOG.warn("A cache save failed", e);
		}
		catch(final TimeoutException e)
		{
			LOG.warn("Timed out while waiting for cache save", e);
		}
		
		LOG.info(
			"Finished waiting for {}x cache saves, took {}ms",
			cfs.size(),
			System.currentTimeMillis() - startMs);
	}
}
