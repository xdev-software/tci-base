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
package software.xdev.tci.db.containers;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import software.xdev.tci.pull.policy.NeverPullPolicy;


class TestQueryStringAccessorTest
{
	@ParameterizedTest
	@MethodSource
	void checkIfAccessorWorks(final String expected, final String provided) throws Exception
	{
		final MockJDBCContainer container = new MockJDBCContainer(
			new RemoteDockerImage(DockerImageName.parse("test" + UUID.randomUUID()))
				.withImagePullPolicy(NeverPullPolicy.INSTANCE), provided);
		Assertions.assertEquals(expected, TestQueryStringAccessor.testQueryString(container));
	}
	
	static Stream<Arguments> checkIfAccessorWorks()
	{
		return Stream.of(
			Arguments.of("SELECT 123", "SELECT 123"),
			Arguments.of(null, null)
		);
	}
	
	static class MockJDBCContainer extends JdbcDatabaseContainer<MockJDBCContainer>
	{
		private final String testQueryString;
		
		MockJDBCContainer(final Future<String> image, final String testQueryString)
		{
			super(image);
			this.testQueryString = testQueryString;
		}
		
		@Override
		public String getDriverClassName()
		{
			return "";
		}
		
		@Override
		public String getJdbcUrl()
		{
			return "";
		}
		
		@Override
		public String getUsername()
		{
			return "";
		}
		
		@Override
		public String getPassword()
		{
			return "";
		}
		
		@Override
		protected String getTestQueryString()
		{
			return this.testQueryString;
		}
	}
}
