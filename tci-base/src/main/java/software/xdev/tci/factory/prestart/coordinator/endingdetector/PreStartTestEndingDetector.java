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
package software.xdev.tci.factory.prestart.coordinator.endingdetector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.LoggerFactory;

import software.xdev.tci.factory.prestart.config.PreStartConfig;
import software.xdev.tci.factory.prestart.coordinator.GlobalPreStartCoordinator;


/**
 * Detects when tests are "ending" by monitoring if there are non-started tests.
 * <p>
 * If all tests have been started the tests are "ending" and the {@link GlobalPreStartCoordinator} is shut down to not
 * PreStart infrastructure that will never be needed.
 * </p>
 */
public class PreStartTestEndingDetector implements TestExecutionListener
{
	private TestPlan currentPlan;
	private final Set<UniqueId> startedIds = Collections.synchronizedSet(new HashSet<>());
	private boolean canShutdown;
	private boolean calledShutdown;
	
	@Override
	public void testPlanExecutionStarted(final TestPlan testPlan)
	{
		if(PreStartConfig.instance().detectEndingTests())
		{
			this.currentPlan = testPlan;
		}
	}
	
	boolean isDisabled()
	{
		return this.currentPlan == null;
	}
	
	@Override
	public void executionSkipped(final TestIdentifier testIdentifier, final String reason)
	{
		this.markAsStartedAndCheckIfEnding(testIdentifier);
	}
	
	@Override
	public void executionStarted(final TestIdentifier testIdentifier)
	{
		this.markAsStartedAndCheckIfEnding(testIdentifier);
	}
	
	void markAsStartedAndCheckIfEnding(final TestIdentifier testIdentifier)
	{
		if(this.isDisabled() || this.calledShutdown)
		{
			return;
		}
		
		this.startedIds.add(testIdentifier.getUniqueIdObject());
		this.checkForShutdown();
	}
	
	@Override
	public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult)
	{
		if(this.isDisabled())
		{
			return;
		}
		
		// Shutdown can only be invoked after at least one test was finished
		if(!this.canShutdown)
		{
			this.canShutdown = true;
			this.checkForShutdown();
		}
	}
	
	void checkForShutdown()
	{
		if(!this.canShutdown || !GlobalPreStartCoordinator.isPresent())
		{
			return;
		}
		
		final long nonStarted = this.currentPlan.countTestIdentifiers(i ->
			!this.startedIds.contains(i.getUniqueIdObject()));
		if(nonStarted == 0)
		{
			this.callShutdown();
		}
	}
	
	synchronized void callShutdown()
	{
		if(!this.calledShutdown)
		{
			LoggerFactory.getLogger(PreStartTestEndingDetector.class)
				.info("Shutting down GlobalPreStartCoordinator as no new tests are pending");
			GlobalPreStartCoordinator.instance().close();
			this.calledShutdown = true;
		}
	}
}
