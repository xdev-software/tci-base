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

import org.testcontainers.containers.GenericContainer;


/**
 * A snapshot represents a container with/at a given state (basically an image or "checkpoint").
 * <p>
 * When multiple containers always require the same configuration (e.g. creating a database and migrating it) this can
 * be used to massively speed up the process.
 * </p>
 * <p>
 * It works like this:
 * <ol>
 *     <li>The initial container is started and configured</li>
 *     <li>The initial container is "snapshoted"</li>
 *     <li>The snapshot is stored somewhere (e.g. as an image) until it's no longer needed</li>
 *     <li>Subsequent containers will be "restored"/created from this snapshot</li>
 * </ol>
 * </p>
 */
public interface SnapshotManager
{
	void tryReuse(final GenericContainer<?> container);
	
	<C extends GenericContainer<?>> void snapshot(final C container);
}
