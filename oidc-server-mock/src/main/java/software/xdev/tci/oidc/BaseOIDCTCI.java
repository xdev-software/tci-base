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
package software.xdev.tci.oidc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.TCI;
import software.xdev.tci.envperf.EnvironmentPerformance;
import software.xdev.tci.oidc.api.OIDCServerMockApi;
import software.xdev.tci.oidc.containers.BaseOIDCServerContainer;


@SuppressWarnings("java:S119")
public abstract class BaseOIDCTCI<
	SELF extends BaseOIDCTCI<SELF, C, A>,
	C extends BaseOIDCServerContainer<C>,
	A extends OIDCServerMockApi>
	extends TCI<C>
{
	private static final Logger LOG = LoggerFactory.getLogger(BaseOIDCTCI.class);
	
	public static final String CLIENT_ID = BaseOIDCServerContainer.DEFAULT_CLIENT_ID;
	public static final String CLIENT_SECRET = BaseOIDCServerContainer.DEFAULT_CLIENT_SECRET;
	
	public static final String DEFAULT_DOMAIN = "example.local";
	
	public static final String DEFAULT_USER_EMAIL = "test@" + DEFAULT_DOMAIN;
	public static final String DEFAULT_USER_NAME = "Testuser";
	public static final String DEFAULT_USER_PASSWORD = "pwd";
	
	protected final Function<SELF, A> apiCreator;
	
	protected A api;
	
	protected boolean createDefaultUser = true;
	protected String defaultUserEmail;
	protected String defaultUserName;
	protected String defaultUserPassword;
	
	protected BaseOIDCTCI(
		final C container,
		final String networkAlias,
		final Function<SELF, A> apiCreator)
	{
		super(container, networkAlias);
		this.apiCreator = apiCreator;
	}
	
	@Override
	public void start(final String containerName)
	{
		super.start(containerName);
		this.api = this.apiCreator.apply(this.self());
		if(this.createDefaultUser)
		{
			this.addDefaultUser();
		}
		
		// Warm up; Otherwise slow initial response may cause a timeout during tests
		this.warmUpWellKnownJWKsEndpoint();
	}
	
	protected void addDefaultUser()
	{
		this.getApi().addUser(this.getDefaultUserEmail(), this.getDefaultUserName(), this.getDefaultUserPassword());
	}
	
	@Override
	public void stop()
	{
		this.closeAndFreeAPI();
		
		super.stop();
	}
	
	protected void closeAndFreeAPI()
	{
		if(this.api != null)
		{
			if(this.api instanceof final AutoCloseable autoCloseableApi)
			{
				try
				{
					autoCloseableApi.close();
				}
				catch(final Exception ex)
				{
					LOG.warn("Failed to close API", ex);
				}
			}
			this.api = null;
		}
	}
	
	public String getDefaultUserEmail()
	{
		return Objects.requireNonNullElse(this.defaultUserEmail, DEFAULT_USER_EMAIL);
	}
	
	public String getDefaultUserName()
	{
		return Objects.requireNonNullElse(this.defaultUserName, DEFAULT_USER_NAME);
	}
	
	public String getDefaultUserPassword()
	{
		return Objects.requireNonNullElse(this.defaultUserPassword, DEFAULT_USER_PASSWORD);
	}
	
	public static String getInternalHttpBaseEndPoint(final String networkAlias)
	{
		return "http://" + networkAlias + ":" + BaseOIDCServerContainer.PORT;
	}
	
	public String getInternalHttpBaseEndPoint()
	{
		return getInternalHttpBaseEndPoint(this.getNetworkAlias());
	}
	
	public String getExternalHttpBaseEndPoint()
	{
		return this.getContainer().getExternalHttpBaseEndPoint();
	}
	
	public void warmUpWellKnownJWKsEndpoint()
	{
		final int slownessFactor = EnvironmentPerformance.cpuSlownessFactor();
		try(final HttpClient warmUpHttpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(1L + slownessFactor))
			.build())
		{
			Unreliables.retryUntilSuccess(
				5 + slownessFactor,
				() -> warmUpHttpClient.send(
					HttpRequest.newBuilder(URI.create(
							this.getExternalHttpBaseEndPoint() + "/.well-known/openid-configuration/jwks"))
						.timeout(Duration.ofSeconds(10L + slownessFactor * 5L))
						.GET()
						.build(),
					HttpResponse.BodyHandlers.discarding()));
		}
	}
	
	public A getApi()
	{
		return this.api;
	}
	
	// region Configure
	
	protected SELF withCreateDefaultUser(final boolean createDefaultUser)
	{
		this.createDefaultUser = createDefaultUser;
		return this.self();
	}
	
	public SELF withDefaultUserEmail(final String defaultUserEmail)
	{
		this.defaultUserEmail = defaultUserEmail;
		return this.self();
	}
	
	public SELF withDefaultUserName(final String defaultUserName)
	{
		this.defaultUserName = defaultUserName;
		return this.self();
	}
	
	public SELF withDefaultUserPassword(final String defaultUserPassword)
	{
		this.defaultUserPassword = defaultUserPassword;
		return this.self();
	}
	
	// endregion
	
	@SuppressWarnings("unchecked")
	public SELF self()
	{
		return (SELF)this;
	}
}
