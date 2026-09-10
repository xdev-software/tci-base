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
package software.xdev.tci.oidc.api;

import org.junit.jupiter.api.BeforeAll;

import software.xdev.tci.network.LazyNetwork;
import software.xdev.tci.network.LazyNetworkPool;
import software.xdev.tci.oidc.BaseOIDCTCI;


public abstract class ApiTest<T extends BaseOIDCTCI<T, ?, ?>>
{
	private static final LazyNetworkPool LAZY_NETWORK_POOL = new LazyNetworkPool();
	
	protected LazyNetwork network;
	protected T oidcInfra;
	
	@BeforeAll
	static void beforeAll()
	{
		LAZY_NETWORK_POOL.managePoolAsync();
	}
	
	protected void beforeEach()
	{
		this.network = LAZY_NETWORK_POOL.getNew();
	}
	
	protected void afterEach()
	{
		if(this.oidcInfra != null)
		{
			this.oidcInfra.stop();
			this.oidcInfra = null;
		}
		if(this.network != null)
		{
			this.network.close();
			this.network = null;
		}
	}
}
