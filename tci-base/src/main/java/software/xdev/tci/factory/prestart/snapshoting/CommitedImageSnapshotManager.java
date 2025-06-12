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
package software.xdev.tci.factory.prestart.snapshoting;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ResourceReaper;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Volume;


/**
 * Uses
 * <a href="https://docs.docker.com/reference/cli/docker/container/commit/"><pre>docker container commit</pre></a>
 * for snapshots.
 * <p>
 * Important notes:
 * <ul>
 *     <li>Only storage is snapshoted, no in-memory data, processes, etc</li>
 *     <li>Volumes are not snapshoted (see {@link #checkForVolumes(GenericContainer)})</li>
 * </ul>
 * </p>
 */
public class CommitedImageSnapshotManager implements SnapshotManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(CommitedImageSnapshotManager.class);
	
	protected Set<String> ignoreWarningsVolumePaths = Set.of();
	protected final ReentrantLock lock = new ReentrantLock();
	protected final AtomicReference<RemoteDockerImage> cachedImage = new AtomicReference<>();
	
	public CommitedImageSnapshotManager()
	{
	}
	
	/**
	 * @see #CommitedImageSnapshotManager(Set)
	 */
	public CommitedImageSnapshotManager(final String... ignoreWarningsVolumePaths)
	{
		this(Set.of(ignoreWarningsVolumePaths));
	}
	
	/**
	 * @see #withIgnoreWarningsVolumePaths(Set)
	 */
	public CommitedImageSnapshotManager(final Set<String> ignoreWarningsVolumePaths)
	{
		this.withIgnoreWarningsVolumePaths(ignoreWarningsVolumePaths);
	}
	
	/**
	 * Add all volumes here that should NOT be warned about when commiting.
	 * <p>
	 * Setting this to {@code null} will disable the warning completely (not recommended).
	 * </p>
	 */
	public CommitedImageSnapshotManager withIgnoreWarningsVolumePaths(final Set<String> ignoreWarningsVolumePaths)
	{
		this.ignoreWarningsVolumePaths = ignoreWarningsVolumePaths;
		return this;
	}
	
	@Override
	public void tryReuse(final GenericContainer<?> container)
	{
		final RemoteDockerImage image = this.cachedImage.get();
		if(image != null)
		{
			LOG.debug("Using cached image {} for {}", image, container.getClass());
			SetImageIntoContainer.instance().accept(container, image);
		}
	}
	
	@Override
	public <C extends GenericContainer<?>> void snapshot(final C container)
	{
		this.commit(container, null, null);
	}
	
	// Before and After-Commit can be used to maybe fully stop/start the container
	// or signal that it should flush everything to disk
	@SuppressWarnings("java:S2629")
	protected <C extends GenericContainer<?>> void commit(
		final C container,
		final Consumer<C> beforeCommit,
		final Consumer<C> afterCommit)
	{
		if(this.cachedImage.get() != null)
		{
			return;
		}
		
		this.lock.lock();
		
		try
		{
			// Recheck if other thread might have already set it
			if(this.cachedImage.get() != null)
			{
				return;
			}
			
			if(beforeCommit != null)
			{
				beforeCommit.accept(container);
			}
			
			this.checkForVolumes(container);
			
			final String name = "commited-cache-"
				+ container.getContainerName()
				.replace("/", "")
				.toLowerCase(Locale.ENGLISH)
				+ "-"
				+ this.hashCode();
			
			@SuppressWarnings({"resource", "java:S1874", "deprecation"})
			final String commitedSha = DockerClientFactory.lazyClient()
				.commitCmd(container.getContainerId())
				.withRepository(name)
				.withLabels(ResourceReaper.instance().getLabels())
				.exec();
			LOG.debug("Created cached image {}/{} for {}", name, commitedSha, container.getContainerName());
			this.cachedImage.set(new RemoteDockerImage(DockerImageName.parse(name))
				.withImagePullPolicy(ignored2 -> false));
			
			if(afterCommit != null)
			{
				afterCommit.accept(container);
			}
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to commit {}", container.getContainerName(), ex);
		}
		finally
		{
			this.lock.unlock();
		}
	}
	
	@SuppressWarnings("java:S2629")
	protected void checkForVolumes(final GenericContainer<?> container)
	{
		if(this.ignoreWarningsVolumePaths == null)
		{
			return;
		}
		
		final List<InspectContainerResponse.Mount> mounts = container.getContainerInfo().getMounts();
		if(mounts != null)
		{
			final List<String> problematicMounts = mounts.stream()
				.map(InspectContainerResponse.Mount::getDestination)
				.filter(Objects::nonNull)
				.map(Volume::getPath)
				.filter(src -> !this.ignoreWarningsVolumePaths.contains(src))
				.sorted()
				.toList();
			if(!problematicMounts.isEmpty())
			{
				LOG.warn(
					"""
						Detected mounts on container that can't be commited: {} (based on {})
						These mounts will NOT be commited and the data can't be reused!
						There is currently no option to disable this inside Images \
						(see https://github.com/moby/moby/issues/43190). \
						You have to manually remove the VOLUME from the image or
						write the data on a different path and add the unused VOLUME to the suppression list here.
						List of problematic mounts:
						{}""",
					container.getContainerName(),
					container.getDockerImageName(),
					String.join("\n", problematicMounts));
			}
		}
	}
}
