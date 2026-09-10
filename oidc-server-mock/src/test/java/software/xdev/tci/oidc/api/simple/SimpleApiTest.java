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
package software.xdev.tci.oidc.api.simple;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.xdev.tci.oidc.OIDCTCI;
import software.xdev.tci.oidc.api.ApiTest;
import software.xdev.tci.oidc.factory.OIDCTCIFactory;


class SimpleApiTest extends ApiTest<OIDCTCI>
{
	protected static final OIDCTCIFactory FACTORY = new OIDCTCIFactory();
	
	@Override
	@BeforeEach
	protected void beforeEach()
	{
		super.beforeEach();
		this.oidcInfra = FACTORY.getNew(this.network);
	}
	
	@Test
	void check()
	{
		final SimpleOIDCServerMockApi api = this.oidcInfra.getApi();
		Assertions.assertDoesNotThrow(() -> {
			final String subjectId = api.addUser("test@nonexisting.localhost", "TestUser", "pwd");
			api.replaceUser(subjectId, "test@nonexisting.localhost", "TestUser", "pwd2");
			api.deleteUser(subjectId);
		});
	}
	
	@Override
	@AfterEach
	protected void afterEach()
	{
		super.afterEach();
	}
	
	@AfterAll
	static void afterAll()
	{
		FACTORY.close();
	}
}
