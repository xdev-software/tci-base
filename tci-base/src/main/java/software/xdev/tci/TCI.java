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
package software.xdev.tci;

import java.util.Objects;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import software.xdev.tci.safestart.SafeNamedContainerStarter;


/**
 * Represents basic Testcontainers based infrastructure.
 * <p>
 * It can be extended and contain other things like e.g. clients, that may also require initialization during startup.
 * </p>
 * <p>
 * This was created so that you don't have to extend containers and add non-container related stuff like e.g. clients to
 * them. ("You should favor composition over inheritance")
 * </p>
 */
@SuppressWarnings("java:S119")
public class TCI<C extends GenericContainer<C>>
{
	private C container;
	private String networkAlias;
	private Runnable onStopped;
	
	protected TCI(final C container, final String networkAlias)
	{
		this.container = Objects.requireNonNull(container);
		this.networkAlias = networkAlias;
	}
	
	public void setOnStopped(final Runnable onStopped)
	{
		this.onStopped = onStopped;
	}
	
	public void setNetworkAlias(final String networkAlias)
	{
		this.networkAlias = networkAlias;
	}
	
	public void start(final String containerName)
	{
		new SafeNamedContainerStarter<>(containerName, this.container).start();
	}
	
	public void stop()
	{
		if(this.container == null) // Already stopped
		{
			return;
		}
		
		try
		{
			Unreliables.retryUntilSuccess(2, () -> {
				this.container.stop();
				return null;
			});
		}
		catch(final Exception ex)
		{
			LoggerFactory.getLogger(this.getClass())
				.warn("Failed to stop container", ex);
		}
		this.container = null;
		this.networkAlias = null;
		this.onStopped();
	}
	
	protected void onStopped()
	{
		if(this.onStopped != null)
		{
			this.onStopped.run();
			this.onStopped = null;
		}
	}
	
	public C getContainer()
	{
		return this.container;
	}
	
	public String getNetworkAlias()
	{
		return this.networkAlias;
	}
}
