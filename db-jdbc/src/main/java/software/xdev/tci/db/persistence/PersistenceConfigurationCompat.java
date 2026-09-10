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
package software.xdev.tci.db.persistence;

//@formatter:off
/**
 * Internal compatibility layer for
 * <a href=
 * "https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/persistenceconfiguration">
 * Jakarta EE v3.2's PersistenceConfiguration
 * </a>
 *
 * @deprecated Use {@link jakarta.persistence.PersistenceConfiguration} instead
 */
//@formatter:on
@Deprecated(forRemoval = true)
public final class PersistenceConfigurationCompat
{
	public static final String JDBC_DRIVER = "jakarta.persistence.jdbc.driver";
	public static final String JDBC_URL = "jakarta.persistence.jdbc.url";
	public static final String JDBC_USER = "jakarta.persistence.jdbc.user";
	public static final String JDBC_PASSWORD = "jakarta.persistence.jdbc.password";
	
	private PersistenceConfigurationCompat()
	{
	}
}
