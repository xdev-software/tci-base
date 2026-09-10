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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import software.xdev.tci.oidc.api.HttpClientBasedOIDCServerMockApi;
import tools.jackson.databind.json.JsonMapper;


public class JacksonOIDCServerMockApi extends HttpClientBasedOIDCServerMockApi
{
	protected static final JsonMapper DEFAULT_MAPPER = new JsonMapper();
	
	public JacksonOIDCServerMockApi(final String externalHttpBaseEndPoint)
	{
		super(externalHttpBaseEndPoint);
	}
	
	protected JsonMapper jsonMapper()
	{
		return DEFAULT_MAPPER;
	}
	
	@Override
	public String addUser(final String email, final String name, final String pw)
	{
		return this.userBuilder()
			.username(name)
			.pw(pw)
			.claims(c -> c
				.addString("email", email)
				.addString("name", name))
			.createNew()
			.subjectId();
	}
	
	public String addUser(final OIDCMockUser user)
	{
		this.apiAddUser(this.jsonMapper().writeValueAsString(user));
		return user.subjectId();
	}
	
	public void replaceUser(final OIDCMockUser user)
	{
		this.apiReplaceUser(this.jsonMapper().writeValueAsString(user));
	}
	
	public boolean deleteUser(final OIDCMockUser user)
	{
		return this.apiDeleteUser(user.subjectId());
	}
	
	public UserBuilder userBuilder()
	{
		return new UserBuilder(this);
	}
	
	public UserBuilder userBuilder(final OIDCMockUser copyFrom)
	{
		return this.userBuilder().copyFrom(copyFrom);
	}
	
	public static class UserBuilder
	{
		protected final JacksonOIDCServerMockApi api;
		
		protected String subjectId;
		protected String username;
		protected String pw;
		// Keep order -> Use LinkedHashMap
		protected Map<String, OIDCMockClaim> claims = new LinkedHashMap<>();
		
		public UserBuilder(final JacksonOIDCServerMockApi api)
		{
			this.api = api;
		}
		
		public UserBuilder copyFrom(final OIDCMockUser user)
		{
			return this.subjectId(user.subjectId())
				.username(user.username())
				.pw(user.pw())
				.claims(user.claims());
		}
		
		public UserBuilder subjectId(final String subjectId)
		{
			this.subjectId = subjectId;
			return this;
		}
		
		public UserBuilder username(final String username)
		{
			this.username = username;
			return this;
		}
		
		public UserBuilder pw(final String pw)
		{
			this.pw = pw;
			return this;
		}
		
		public UserBuilder claims(final Collection<OIDCMockClaim> claims)
		{
			this.claims.clear();
			claims.forEach(c -> this.claims.put(c.type(), c));
			return this;
		}
		
		public UserBuilder claims(final Consumer<ClaimsBuilder> consumer)
		{
			consumer.accept(new ClaimsBuilder(this.api.jsonMapper(), this.claims));
			return this;
		}
		
		protected List<OIDCMockClaim> getClaims()
		{
			return List.copyOf(this.claims.values());
		}
		
		public OIDCMockUser createNew()
		{
			final OIDCMockUser user = new OIDCMockUser(
				this.api.nextSubjectId(),
				this.username,
				this.pw,
				this.getClaims());
			
			this.api.addUser(user);
			return user;
		}
		
		public OIDCMockUser update()
		{
			final OIDCMockUser user = new OIDCMockUser(
				this.subjectId,
				this.username,
				this.pw,
				this.getClaims());
			
			this.api.replaceUser(user);
			return user;
		}
		
		public boolean delete()
		{
			return this.api.deleteUser(this.subjectId);
		}
	}
	
	
	public static class ClaimsBuilder
	{
		protected final JsonMapper jsonMapper;
		protected final Map<String, OIDCMockClaim> claims;
		
		public ClaimsBuilder(final JsonMapper jsonMapper, final Map<String, OIDCMockClaim> claims)
		{
			this.jsonMapper = jsonMapper;
			this.claims = claims;
		}
		
		public ClaimsBuilder clear()
		{
			this.claims.clear();
			return this;
		}
		
		public ClaimsBuilder remove(final OIDCMockClaim claim)
		{
			return this.remove(claim.type());
		}
		
		public ClaimsBuilder remove(final String type)
		{
			this.claims.remove(type);
			return this;
		}
		
		public ClaimsBuilder add(final OIDCMockClaim claim)
		{
			this.claims.put(claim.type(), claim);
			return this;
		}
		
		public ClaimsBuilder addEmail(final String email)
		{
			return this.addString("email", email);
		}
		
		public ClaimsBuilder addName(final String name)
		{
			return this.addString("name", name);
		}
		
		public ClaimsBuilder addString(final String type, final String value)
		{
			return this.add(OIDCMockClaim.string(type, value));
		}
		
		public ClaimsBuilder addJsonArrayFromNativeStrings(
			final String type,
			final Collection<String> value)
		{
			return this.add(OIDCMockClaim.jsonArrayFromNativeStrings(type, value));
		}
		
		public <T> ClaimsBuilder addJsonArray(
			final String type,
			final Collection<T> value)
		{
			return this.add(OIDCMockClaim.jsonArray(this.jsonMapper, type, value));
		}
	}
}
