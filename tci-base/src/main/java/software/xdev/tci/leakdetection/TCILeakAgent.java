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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.TCIFactory;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;


public class TCILeakAgent implements TestExecutionListener
{
	private static final Logger LOG = LoggerFactory.getLogger(TCILeakAgent.class);
	
	@Override
	public void testPlanExecutionStarted(final TestPlan testPlan)
	{
		LOG.debug("Registered");
	}
	
	@SuppressWarnings({"java:S2629", "java:S106"})
	@Override
	public void testPlanExecutionFinished(final TestPlan testPlan)
	{
		final List<LeakDetectionAsyncReaper> pendingReapers = ServiceLoader.load(LeakDetectionAsyncReaper.class)
			.stream()
			.map(ServiceLoader.Provider::get)
			.toList();
		if(!pendingReapers.isEmpty())
		{
			LOG.info("Waiting for reapers to finish...");
			pendingReapers.forEach(LeakDetectionAsyncReaper::blockUntilReaped);
		}
		
		final Map<TCIFactory<?, ?>, Set<TCI<?>>> leaked = TCIFactoryRegistry.instance().getReturnedAndInUse();
		if(leaked.isEmpty())
		{
			LOG.info("No leaks detected");
			return;
		}
		
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
					+ e.getValue().stream()
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
