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
package software.xdev.tci.mockserver.containers;

import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

import software.xdev.tci.misc.ContainerMemory;
import software.xdev.tci.startup.error.java.fatal.HsErrPidStartUpCrashReporter;
import software.xdev.tci.startup.wait.FastAbortOnContainerDeathWaitStrategy;
import software.xdev.tci.startup.wait.strategy.LogMessageWaitAbortableStrategy;
import software.xdev.testcontainers.mockserver.containers.MockServerContainer;


@SuppressWarnings("java:S2160")
public class TCIMockserverContainer extends MockServerContainer
{
	private final HsErrPidStartUpCrashReporter hsErrPidStartUpCrashReporter;
	
	public TCIMockserverContainer(final RemoteDockerImage image)
	{
		super(image);
		this.hsErrPidStartUpCrashReporter = new HsErrPidStartUpCrashReporter(this);
	}
	
	public TCIMockserverContainer(final DockerImageName dockerImageName)
	{
		super(dockerImageName);
		this.hsErrPidStartUpCrashReporter = new HsErrPidStartUpCrashReporter(this);
	}
	
	public TCIMockserverContainer(final String tag)
	{
		super(tag);
		this.hsErrPidStartUpCrashReporter = new HsErrPidStartUpCrashReporter(this);
	}
	
	public TCIMockserverContainer()
	{
		this.hsErrPidStartUpCrashReporter = new HsErrPidStartUpCrashReporter(this);
	}
	
	@Override
	protected void containerIsStarted(final InspectContainerResponse containerInfo, final boolean reused)
	{
		this.hsErrPidStartUpCrashReporter.containerIsStarted();
		super.containerIsStarted(containerInfo, reused);
	}
	
	@Override
	protected void containerIsStopping(final InspectContainerResponse containerInfo)
	{
		this.hsErrPidStartUpCrashReporter.containerIsStopping(this.logger());
		super.containerIsStopping(containerInfo);
	}
	
	public static TCIMockserverContainer createDefaultForFactory()
	{
		final TCIMockserverContainer container = new TCIMockserverContainer();
		container
			.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemory(ContainerMemory.M512M))
			.waitingFor(new FastAbortOnContainerDeathWaitStrategy(new LogMessageWaitAbortableStrategy()
				.withRegEx(MockServerContainer.LOG_MSG_WAIT_STRATEGY_REGEX)
			));
		return container;
	}
}
