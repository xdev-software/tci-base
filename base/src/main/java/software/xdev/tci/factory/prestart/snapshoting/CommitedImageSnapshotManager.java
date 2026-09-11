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

import software.xdev.tci.pull.policy.NeverPullPolicy;


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
	protected boolean waitForFirstSnapshot = true;
	protected String commitedImagePrefix = "commited-cache";
	
	protected final ReentrantLock commitLock = new ReentrantLock();
	protected final AtomicReference<RemoteDockerImage> cachedImage = new AtomicReference<>();
	
	protected final ReentrantLock waitForFirstSnapshotLock = new ReentrantLock();
	protected final AtomicReference<GenericContainer<?>> waitForFirstSnapshotContainer = new AtomicReference<>();
	
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
	
	/**
	 * Will wait for the first snapshot to be created and block any further calls on
	 * {@link #tryReuse(GenericContainer)}
	 * until the snapshot was created.
	 * <p>
	 * <b>Enabled by default</b>
	 * </p>
	 * <p/>
	 * This is designed to prevent (CPU) bottlenecks during the initial start.
	 * <p>
	 * Example: 50 Containers are totally needed by all tests. Parallelism = 10; Starting a container consumes 20% CPU;
	 * a snapshotted container start only needs 5%
	 * <ul>
	 *     <li>
	 *         <b>Without</b> waiting
	 *         <ul>
	 *             <li>Start 10 Containers in parallel, each one needs 20% CPU</li>
	 *             <li>200% CPU load during start</li>
	 *             <li>Each container takes a long time to start (let's say 30s)</li>
	 *             <li>Snapshot one</li>
	 *             <li>All subsequent containers only need 5% CPU / 10s for starting</li>
	 *             <li><i>Overall start behavior: duration=~30s; 200% CPU load</i></li>
	 *         </ul>
	 *     </li>
	 *     <li>
	 *         <b>With</b> waiting
	 *         <ul>
	 *             <li>Start 10 Containers in parallel</li>
	 *             <li>Only 1 Container will be actually started for the snapshot</li>
	 *             <li>20% CPU load during start</li>
	 *             <li>Container is started and snapshotted in 15s</li>
	 *             <li>Resume the 9 other blocked containers, but they now reuse the snapshot</li>
	 *             <li>All subsequent containers only need 5% CPU / 10s for starting</li>
	 *             <li><i>Overall start behavior: duration=~15-20s; 65% CPU load</i></li>
	 *         </ul>
	 *     </li>
	 * </ul>
	 * </p>
	 */
	public CommitedImageSnapshotManager withWaitForFirstSnapshot(final boolean waitForFirstSnapshot)
	{
		this.waitForFirstSnapshot = waitForFirstSnapshot;
		return this;
	}
	
	public CommitedImageSnapshotManager withCommitedImagePrefix(final String commitedImagePrefix)
	{
		this.commitedImagePrefix = commitedImagePrefix;
		return this;
	}
	
	@Override
	public void tryReuse(final GenericContainer<?> container)
	{
		final RemoteDockerImage image = this.getImageAndMaybeWaitForFirstSnapshot(container);
		if(image != null)
		{
			LOG.debug("Using cached image {} for {}", image, container.getClass());
			SetImageIntoContainer.instance().accept(container, image);
		}
	}
	
	protected RemoteDockerImage getImageAndMaybeWaitForFirstSnapshot(final GenericContainer<?> container)
	{
		RemoteDockerImage image = this.cachedImage.get();
		if(this.waitForFirstSnapshot && image == null)
		{
			LOG.info("Will wait for first snapshot for {}", container.getClass());
			this.waitForFirstSnapshotLock.lock();
			image = this.cachedImage.get();
			if(image == null)
			{
				// Use this container to create the snapshot
				this.waitForFirstSnapshotContainer.set(container);
				LOG.debug(
					"Will try to create first snapshot for {} with details: {}",
					container.getClass(),
					container);
			}
			else
			{
				// We already created the image -> go on
				this.waitForFirstSnapshotLock.unlock();
			}
		}
		return image;
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
		
		this.commitLock.lock();
		
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
			
			final String name = this.commitedImagePrefix
				+ "-"
				+ container.getContainerName()
				.replace("/", "")
				.toLowerCase(Locale.ENGLISH)
				+ "-"
				+ this.hashCode();
			
			LOG.info("Creating cached image {} for {}", name, container.getContainerName());
			
			@SuppressWarnings({"resource", "java:S1874", "deprecation"})
			final String commitedSha = DockerClientFactory.lazyClient()
				.commitCmd(container.getContainerId())
				.withRepository(name)
				.withLabels(ResourceReaper.instance().getLabels())
				.exec();
			LOG.info("Created cached image {}/{} for {}", name, commitedSha, container.getContainerName());
			this.cachedImage.set(new RemoteDockerImage(DockerImageName.parse(name))
				.withImagePullPolicy(NeverPullPolicy.INSTANCE));
			
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
			this.unlockWaitForFirstSnapshotIfRequired(container);
			this.commitLock.unlock();
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
	
	@Override
	public void snapshotFailed(final GenericContainer<?> container, final Exception ex)
	{
		this.unlockWaitForFirstSnapshotIfRequired(container);
	}
	
	protected void unlockWaitForFirstSnapshotIfRequired(final GenericContainer<?> container)
	{
		if(this.waitForFirstSnapshot && this.waitForFirstSnapshotContainer.get() == container)
		{
			this.waitForFirstSnapshotLock.unlock();
			this.waitForFirstSnapshotContainer.set(null);
			LOG.info("Unlocked wait for first snapshot {}", container.getClass());
		}
	}
}
