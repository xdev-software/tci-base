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
package software.xdev.tci.mailpit;

import java.time.Duration;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.LoggerFactory;

import software.xdev.mailpit.client.ApiClient;
import software.xdev.tci.TCI;
import software.xdev.tci.mailpit.containers.MailpitContainer;


public class MailpitTCI extends TCI<MailpitContainer>
{
	protected ApiClient apiClient;
	
	public MailpitTCI(final MailpitContainer container, final String networkAlias)
	{
		super(container, networkAlias);
	}
	
	public String getExternalHTTPEndpoint()
	{
		return "http://"
			+ this.getContainer().getHost()
			+ ":"
			+ this.getContainer().getMappedPort(MailpitContainer.WEB_PORT);
	}
	
	public ApiClient apiClient()
	{
		if(this.apiClient != null)
		{
			return this.apiClient;
		}
		
		final Duration defaultTimeout = Duration.ofSeconds(30);
		
		this.apiClient = new ApiClient();
		this.apiClient.setHttpClient(HttpClientBuilder.create()
			.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(ConnectionConfig.custom()
					.setConnectTimeout(Timeout.of(defaultTimeout))
					.setSocketTimeout(Timeout.of(defaultTimeout))
					.build())
				.build())
			.build());
		this.apiClient.setBasePath(this.getExternalHTTPEndpoint());
		
		return this.apiClient;
	}
	
	@Override
	public void stop()
	{
		if(this.apiClient != null)
		{
			try
			{
				this.apiClient.getHttpClient().close();
			}
			catch(final Exception e)
			{
				LoggerFactory.getLogger(this.getClass()).warn("Failed to close API client", e);
			}
		}
		super.stop();
	}
}
