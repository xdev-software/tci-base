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
package software.xdev.tci.imagebuild.cache.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class TCICacheSaveRegistry
{
	protected static TCICacheSaveRegistry instance;
	
	public static TCICacheSaveRegistry instance()
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
			instance = new TCICacheSaveRegistry();
		}
	}
	
	protected final List<CompletableFuture<?>> cfs = Collections.synchronizedList(new ArrayList<>());
	
	public void add(final CompletableFuture<?> cf)
	{
		this.cfs.add(cf);
	}
	
	public List<CompletableFuture<?>> get()
	{
		return this.cfs;
	}
	
	protected TCICacheSaveRegistry()
	{
	}
}
