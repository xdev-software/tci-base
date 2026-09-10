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
package software.xdev.tci.selenium.containers;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.openqa.selenium.Capabilities;
import org.testcontainers.utility.DockerImageName;

import software.xdev.tci.portfixation.AdditionalPortsForFixedExposingContainer;
import software.xdev.tci.safestart.SafeNamedContainerStarter;
import software.xdev.testcontainers.selenium.containers.browser.CapabilitiesBrowserWebDriverContainer;


public class SeleniumBrowserWebDriverContainer
	extends CapabilitiesBrowserWebDriverContainer<SeleniumBrowserWebDriverContainer>
	implements AdditionalPortsForFixedExposingContainer
{
	public SeleniumBrowserWebDriverContainer(final Capabilities capabilities)
	{
		super(
			capabilities, Map.of(
				// Limit to the core (and most open source) browsers by distinct engines
				// 1. Firefox uses Gecko
				BrowserType.FIREFOX, FIREFOX_IMAGE,
				// 2. Everything else is running Chromium/Blink
				// Chrome has no ARM64 image (embarrassing) -> Use chromium instead
				// https://github.com/SeleniumHQ/docker-selenium/discussions/2379
				BrowserType.CHROME, CHROMIUM_IMAGE
				// 3. Safari/Webkit is N/A because Apple is doing Apple stuff
				// https://github.com/SeleniumHQ/docker-selenium/issues/1635
				// 4. IE/Trident/EdgeHTML is dead
				// 5. Everything else is irrelevant
			));
	}
	
	public SeleniumBrowserWebDriverContainer(
		final Capabilities capabilities,
		final Map<String, DockerImageName> browserDockerImages)
	{
		super(capabilities, browserDockerImages);
	}
	
	public SeleniumBrowserWebDriverContainer(final DockerImageName dockerImageName)
	{
		super(dockerImageName);
	}
	
	@Override
	public Set<Integer> getAdditionalTCPPortsForFixedExposing()
	{
		final Set<Integer> ports = new HashSet<>(Set.of(SELENIUM_PORT));
		if(!this.disableVNC)
		{
			if(this.exposeVNCPort)
			{
				ports.add(VNC_PORT);
			}
			if(this.enableNoVNC)
			{
				ports.add(NO_VNC_PORT);
			}
		}
		return ports;
	}
	
	@Override
	protected void doStart()
	{
		super.doStart();
		if(this.recordingContainer != null)
		{
			this.startRecordingContainerInternal();
			this.dockerClient.pauseContainerCmd(this.recordingContainer.getContainerId()).exec();
		}
	}
	
	protected void startRecordingContainerInternal()
	{
		Optional.ofNullable(this.recordingContainer)
			.map(c -> new SafeNamedContainerStarter<>("recorder-" + this.getContainerNameCleaned(), c)
				.withAttachRandomUUID(false))
			.ifPresent(SafeNamedContainerStarter::start);
	}
	
	@Override
	public void startRecordingContainer()
	{
		if(this.recordingContainer != null)
		{
			this.dockerClient.unpauseContainerCmd(this.recordingContainer.getContainerId()).exec();
		}
	}
}
