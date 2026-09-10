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
package software.xdev.tci.oidc.factory;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import software.xdev.tci.factory.prestart.config.PreStartConfig;
import software.xdev.tci.oidc.OIDCTCI;
import software.xdev.tci.oidc.containers.OIDCServerContainer;


public class OIDCTCIFactory extends BaseOIDCTCIFactory<OIDCTCIFactory, OIDCServerContainer, OIDCTCI>
{
	public OIDCTCIFactory(
		final BiFunction<OIDCServerContainer, String, OIDCTCI> infraBuilder,
		final Supplier<OIDCServerContainer> containerBuilder)
	{
		super(infraBuilder, containerBuilder);
	}
	
	public OIDCTCIFactory(
		final BiFunction<OIDCServerContainer, String, OIDCTCI> infraBuilder,
		final Supplier<OIDCServerContainer> containerBuilder,
		final PreStartConfig config,
		final Timeouts timeouts)
	{
		super(infraBuilder, containerBuilder, config, timeouts);
	}
	
	public OIDCTCIFactory(
		final BiFunction<OIDCServerContainer, String, OIDCTCI> infraBuilder,
		final Supplier<OIDCServerContainer> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName,
		final String name)
	{
		super(infraBuilder, containerBuilder, containerBaseName, containerLoggerName, name);
	}
	
	public OIDCTCIFactory(
		final BiFunction<OIDCServerContainer, String, OIDCTCI> infraBuilder,
		final Supplier<OIDCServerContainer> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName,
		final String name,
		final PreStartConfig config,
		final Timeouts timeouts)
	{
		super(infraBuilder, containerBuilder, containerBaseName, containerLoggerName, name, config, timeouts);
	}
	
	public OIDCTCIFactory()
	{
		this(OIDCTCI::new);
	}
	
	public OIDCTCIFactory(final BiFunction<OIDCServerContainer, String, OIDCTCI> infraBuilder)
	{
		super(
			infraBuilder,
			BaseOIDCTCIFactory::createDefaultContainer,
			DEFAULT_CONTAINER_BASE_NAME,
			DEFAULT_CONTAINER_LOGGER_NAME,
			DEFAULT_NAME);
	}
}
