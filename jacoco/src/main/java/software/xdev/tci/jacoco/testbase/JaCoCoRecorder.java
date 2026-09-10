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
package software.xdev.tci.jacoco.testbase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import software.xdev.tci.TCI;
import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.jacoco.containers.JaCoCoAwareContainer;
import software.xdev.tci.jacoco.testbase.config.JaCoCoConfig;


public class JaCoCoRecorder
{
	private static final Logger LOG = LoggerFactory.getLogger(JaCoCoRecorder.class);
	
	protected static JaCoCoRecorder instance;
	
	public static JaCoCoRecorder instance()
	{
		if(instance == null)
		{
			createDefaultInstance();
		}
		return instance;
	}
	
	protected static synchronized void createDefaultInstance()
	{
		if(instance == null)
		{
			instance = new JaCoCoRecorder();
		}
	}
	
	protected JaCoCoConfig config;
	protected Map<String, Path> cachedDirForExecutionDataFilesVariantPaths = new ConcurrentHashMap<>();
	
	public JaCoCoRecorder()
	{
		this(JaCoCoConfig.instance());
	}
	
	public JaCoCoRecorder(final JaCoCoConfig config)
	{
		this.config = config;
	}
	
	public CompletableFuture<Void> afterTestAsync(
		final Optional<TCI<?>> optTCI,
		final Supplier<String> fileSystemFriendlyNameSupplier)
	{
		return this.afterTestAsync(optTCI, fileSystemFriendlyNameSupplier, null);
	}
	
	public CompletableFuture<Void> afterTestAsync(
		final Optional<TCI<?>> optTCI,
		final Supplier<String> fileSystemFriendlyNameSupplier,
		final String variantName)
	{
		return this.afterTestAsync(optTCI, null, fileSystemFriendlyNameSupplier, variantName);
	}
	
	public CompletableFuture<Void> afterTestAsync(
		final Optional<TCI<?>> optTCI,
		final String jaCoCoExecutionDataFilePathInContainer,
		final Supplier<String> fileSystemFriendlyNameSupplier,
		final String variantName)
	{
		if(!this.config.enabled() || optTCI.isEmpty())
		{
			return CompletableFuture.completedFuture(null);
		}
		
		final GenericContainer<?> container = optTCI.orElseThrow().getContainer();
		if(container == null)
		{
			LOG.debug("No container available");
			return CompletableFuture.completedFuture(null);
		}
		
		final String containerPath =
			jaCoCoExecutionDataFilePathInContainer == null
				&& container instanceof final JaCoCoAwareContainer jaCoCoAwareContainer
				? jaCoCoAwareContainer.jaCoCoExecutionDataFilePathInContainer()
				: jaCoCoExecutionDataFilePathInContainer;
		if(containerPath == null)
		{
			LOG.warn(
				"Unabled to determine container path for {}. "
					+ "Forgot to implement JaCoCoAwareContainer or to specify parameter?",
				container);
			return CompletableFuture.completedFuture(null);
		}
		
		return CompletableFuture.runAsync(
			() -> this.stopContainerAndCopy(fileSystemFriendlyNameSupplier, variantName, container, containerPath),
			TCIExecutorServiceHolder.instance()
		);
	}
	
	protected void stopContainerAndCopy(
		final Supplier<String> fileSystemFriendlyNameSupplier,
		final String variantName,
		final GenericContainer<?> container,
		final String containerPath)
	{
		this.stopContainer(container);
		
		try
		{
			LOG.debug("Trying to extract JaCoCo execution data file");
			
			container.copyFileFromContainer(
				containerPath,
				is -> {
					Files.copy(
						is,
						this.resolveDirForExecutionDataFiles(variantName)
							.resolve(
								fileSystemFriendlyNameSupplier.get() + this.config.executionDataFileSuffix()),
						StandardCopyOption.REPLACE_EXISTING);
					return null;
				});
			LOG.debug("Finished extraction of JaCoCo execution data file");
		}
		catch(final Exception ex)
		{
			LOG.warn("Unable to copy JaCoCo execution data file", ex);
		}
	}
	
	@SuppressWarnings("resource")
	protected void stopContainer(final GenericContainer<?> container)
	{
		if(container.isRunning())
		{
			// Shutdown container so that jacoco agent dumps the execution data file
			try
			{
				DockerClientFactory.lazyClient().stopContainerCmd(container.getContainerId()).exec();
			}
			catch(final Exception ex)
			{
				LOG.warn("Failed to stop container", ex);
			}
		}
	}
	
	protected Path resolveDirForExecutionDataFiles(final String variantName)
	{
		return this.cachedDirForExecutionDataFilesVariantPaths.computeIfAbsent(
			Objects.requireNonNullElse(variantName, "app"),
			v ->
			{
				final Path dir = this.config.dirForExecutionDataFiles().resolve(v);
				final boolean wasCreated = dir.toFile().mkdirs();
				
				LOG.debug(
					"Variant '{}' directory for execution-data-files='{}', wasCreated={}",
					v,
					dir,
					wasCreated);
				return dir;
			});
	}
}
