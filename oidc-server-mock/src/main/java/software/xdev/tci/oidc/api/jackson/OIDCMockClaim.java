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
package software.xdev.tci.oidc.api.jackson;

import java.util.Collection;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.json.JsonMapper;


public record OIDCMockClaim(
	@JsonProperty("Type")
	String type,
	@JsonProperty("Value")
	String value,
	@JsonProperty("ValueType")
	String valueType
)
{
	public static OIDCMockClaim string(final String type, final String value)
	{
		return new OIDCMockClaim(type, value, "string");
	}
	
	public static OIDCMockClaim jsonArrayFromNativeStrings(
		final String type,
		final Collection<String> value)
	{
		return new OIDCMockClaim(
			type, "["
			+ value.stream()
			.map(s -> "\"" + s + "\"")
			.collect(Collectors.joining(", "))
			+ "]",
			"json");
	}
	
	public static <T> OIDCMockClaim jsonArray(
		final JsonMapper jsonMapper,
		final String type,
		final Collection<T> value)
	{
		return new OIDCMockClaim(
			type,
			jsonMapper.writeValueAsString(value),
			"json");
	}
}
