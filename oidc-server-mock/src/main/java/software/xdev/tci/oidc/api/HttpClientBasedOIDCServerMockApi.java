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
package software.xdev.tci.oidc.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;


public abstract class HttpClientBasedOIDCServerMockApi
	extends OIDCServerMockApi implements AutoCloseable
{
	protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
	
	protected final HttpClient httpClient;
	protected final URI externalHttpBaseEndPointUri;
	
	public HttpClientBasedOIDCServerMockApi(final String externalHttpBaseEndPoint)
	{
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(DEFAULT_TIMEOUT)
			.build();
		this.externalHttpBaseEndPointUri = URI.create(externalHttpBaseEndPoint);
	}
	
	protected void apiAddUser(final String jsonBody)
	{
		this.apiAddUser(jsonBody, false);
	}
	
	protected void apiAddUser(final String jsonBody, final boolean validate)
	{
		this.apiAddOrReplaceUser("create", HttpRequest.Builder::POST, jsonBody, validate ? "?validate=true" : "");
	}
	
	protected void apiReplaceUser(final String jsonBody)
	{
		this.apiAddOrReplaceUser("replace", HttpRequest.Builder::PUT, jsonBody, "");
	}
	
	protected void apiAddOrReplaceUser(
		final String operation,
		final BiFunction<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> method,
		final String jsonBody,
		final String additionalPath)
	{
		final HttpResponse<String> response = this.sendRequest(
			"/api/v1/user" + additionalPath,
			b -> method.apply(
				b
					.header("Accept", "application/json")
					.header("Content-Type", "application/json"),
				HttpRequest.BodyPublishers.ofString(jsonBody)),
			HttpResponse.BodyHandlers.ofString());
		if(!this.isApiResponseOk(response))
		{
			throw new IllegalStateException("Unable to " + operation + " user; Expected statuscode 2XX but got "
				+ response.statusCode()
				+ "; Message: " + response.body());
		}
	}
	
	public boolean deleteUser(final String subjectId)
	{
		return this.apiDeleteUser(subjectId);
	}
	
	protected boolean apiDeleteUser(final String subjectId)
	{
		return this.isApiResponseOk(this.sendRequest(
			"/api/v1/user/" + subjectId,
			HttpRequest.Builder::DELETE));
	}
	
	protected HttpResponse<Void> sendRequest(
		final String relativePath,
		final UnaryOperator<HttpRequest.Builder> buildRequest)
	{
		return this.sendRequest(relativePath, buildRequest, HttpResponse.BodyHandlers.discarding());
	}
	
	protected <T> HttpResponse<T> sendRequest(
		final String relativePath,
		final UnaryOperator<HttpRequest.Builder> buildRequest,
		final HttpResponse.BodyHandler<T> bodyHandler)
	{
		try
		{
			return this.httpClient.send(
				buildRequest.apply(HttpRequest.newBuilder(
							this.externalHttpBaseEndPointUri.resolve(relativePath))
						.timeout(DEFAULT_TIMEOUT))
					.build(),
				bodyHandler);
		}
		catch(final InterruptedException iex)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Got interrupted", iex);
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	protected boolean isApiResponseOk(final HttpResponse<?> response)
	{
		final int code = response.statusCode();
		return code >= 200 && code < 300;
	}
	
	@Override
	public void close()
	{
		if(this.httpClient != null)
		{
			this.httpClient.close();
		}
	}
}
