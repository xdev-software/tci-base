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
package software.xdev.tci.config;

import java.util.Locale;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.slf4j.LoggerFactory;


public abstract class DefaultConfig
{
	protected abstract String propertyNamePrefix();
	
	protected Optional<String> resolve(final String propertyName)
	{
		return this.resolveFullyBuildPropertyName(this.propertyNamePrefix() + "." + propertyName);
	}
	
	protected Optional<String> resolveFullyBuildPropertyName(final String fullPropertyName)
	{
		return Optional.ofNullable(System.getenv(fullPropertyName
				.replace(".", "_")
				.toUpperCase(Locale.ROOT)))
			.or(() -> Optional.ofNullable(System.getProperty(fullPropertyName)));
	}
	
	protected boolean resolveBool(final String propertyName, final boolean defaultVal)
	{
		return this.resolveBool(propertyName)
			.orElse(defaultVal);
	}
	
	protected Optional<Boolean> resolveBool(final String propertyName)
	{
		return this.resolve(propertyName)
			.map(s -> "1".equals(s) || Boolean.parseBoolean(s));
	}
	
	protected int resolveInt(final String propertyName, final int defaultVal)
	{
		return this.resolveInt(propertyName, () -> defaultVal);
	}
	
	protected int resolveInt(final String propertyName, final IntSupplier defaultValueSupplier)
	{
		return this.resolve(propertyName)
			.map(s -> {
				try
				{
					return Integer.parseInt(s);
				}
				catch(final NumberFormatException nfe)
				{
					return null;
				}
			})
			.orElseGet(defaultValueSupplier::getAsInt);
	}
	
	protected long resolveLong(final String propertyName, final LongSupplier defaultValueSupplier)
	{
		return this.resolve(propertyName)
			.map(s -> {
				try
				{
					return Long.parseLong(s);
				}
				catch(final NumberFormatException nfe)
				{
					return null;
				}
			})
			.orElseGet(defaultValueSupplier::getAsLong);
	}
	
	protected <T> T reportLegacyConfigOption(final String legacy, final String upToDate, final T value)
	{
		this.reportLegacyConfigOption(legacy, upToDate);
		return value;
	}
	
	protected void reportLegacyConfigOption(final String legacy, final String upToDate)
	{
		LoggerFactory.getLogger(this.getClass()).warn(
			"Detected deprecated config option that will be removed: {} - use {} instead",
			legacy,
			upToDate);
	}
}
