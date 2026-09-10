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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


public record OIDCMockUser(
	@JsonProperty("SubjectId")
	String subjectId,
	@JsonProperty("Username")
	String username,
	@JsonProperty("Password")
	String pw,
	@JsonProperty("Claims")
	List<OIDCMockClaim> claims
)
{
}
