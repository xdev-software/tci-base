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
package software.xdev.tci.tracing;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.factory.TCIFactory;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.serviceloading.TCIServiceLoader;
import software.xdev.tci.tracing.config.TracingConfig;


/**
 * Traces various TCI metrics including, average duration, amount and total duration of
 * <ul>
 *     <li>tests</li>
 *     <li>containers</li>
 *     <li>pre-starting</li>
 *     <li>...</li>
 * </ul>
 * <p>
 * Active by default due to service loading.
 * </p>
 */
public class TCITracingAgent implements TestExecutionListener
{
	private static final Logger LOG = LoggerFactory.getLogger(TCITracingAgent.class);
	
	protected TracingConfig config;
	protected long startTime;
	
	protected final TCITracer.Timed testsTimed = new TCITracer.Timed();
	protected final Map<TestIdentifier, Long> testStartTime = Collections.synchronizedMap(new HashMap<>());
	
	@Override
	public void testPlanExecutionStarted(final TestPlan testPlan)
	{
		this.config = TCIServiceLoader.instance().service(TracingConfig.class);
		if(!this.config.enabled())
		{
			return;
		}
		
		this.startTime = System.currentTimeMillis();
	}
	
	@Override
	public void executionStarted(final TestIdentifier testIdentifier)
	{
		if(!this.config.enabled())
		{
			return;
		}
		
		if(testIdentifier.getType() != TestDescriptor.Type.CONTAINER)
		{
			this.testStartTime.put(testIdentifier, System.currentTimeMillis());
		}
	}
	
	@Override
	public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult)
	{
		if(!this.config.enabled())
		{
			return;
		}
		
		if(testIdentifier.getType() != TestDescriptor.Type.CONTAINER)
		{
			Optional.ofNullable(this.testStartTime.remove(testIdentifier))
				.ifPresent(s -> this.testsTimed.addMs(System.currentTimeMillis() - s));
		}
	}
	
	@Override
	public void testPlanExecutionFinished(final TestPlan testPlan)
	{
		if(!this.config.enabled())
		{
			return;
		}
		
		final long allTestsExecutionMs = System.currentTimeMillis() - this.startTime;
		final String message = "=== Test Tracing Info ===\n"
			+ "Duration: " + TCITracer.Timed.prettyPrintMS(allTestsExecutionMs) + "\n"
			+ "Tests: " + this.testsTimed + "\n"
			+ TCIFactoryRegistry.instance().getFactories()
			.stream()
			.sorted(Comparator.comparing(TCIFactory::getFactoryName))
			.map(f -> f.getFactoryName()
				+ ":\n"
				+ Optional.ofNullable(f.getTracer())
				.map(t -> t.getTimers().entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.map(e -> "\t" + e.getKey() + " - " + e.getValue())
					.collect(Collectors.joining("\n")))
				.orElse("-"))
			.collect(Collectors.joining("\n"));
		LOG.info(message);
	}
}
