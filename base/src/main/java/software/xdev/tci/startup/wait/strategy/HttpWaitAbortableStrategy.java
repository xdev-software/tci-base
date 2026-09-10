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
package software.xdev.tci.startup.wait.strategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.rnorth.ducttape.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

import software.xdev.tci.startup.wait.AbortMonitor;


/**
 * Based on {@link org.testcontainers.containers.wait.strategy.HttpWaitStrategy}
 */
@SuppressWarnings("PMD.GodClass")
public class HttpWaitAbortableStrategy extends AbstractWaitAbortableStrategy<HttpWaitAbortableStrategy>
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpWaitAbortableStrategy.class);
	
	/**
	 * Authorization HTTP header.
	 */
	protected static final String HEADER_AUTHORIZATION = "Authorization";
	
	/**
	 * Basic Authorization scheme prefix.
	 */
	protected static final String AUTH_BASIC = "Basic ";
	
	protected String path = "/";
	
	protected String method = "GET";
	
	protected final Set<Integer> statusCodes = new HashSet<>();
	
	protected boolean tlsEnabled;
	
	protected String username;
	
	protected String password;
	
	protected final Map<String, String> headers = new HashMap<>();
	
	protected Predicate<String> responsePredicate;
	
	protected Predicate<Integer> statusCodePredicate;
	
	protected Optional<Integer> livenessPort = Optional.empty();
	
	protected Duration readTimeout = Duration.ofSeconds(1);
	
	protected boolean allowInsecure;
	
	// region Configure
	
	/**
	 * Waits for the given status code.
	 *
	 * @param statusCode the expected status code
	 * @return this
	 */
	public HttpWaitAbortableStrategy forStatusCode(final int statusCode)
	{
		this.statusCodes.add(statusCode);
		return this.self();
	}
	
	/**
	 * Waits for the status code to pass the given predicate
	 *
	 * @param statusCodePredicate The predicate to test the response against
	 * @return this
	 */
	public HttpWaitAbortableStrategy forStatusCodeMatching(final Predicate<Integer> statusCodePredicate)
	{
		this.statusCodePredicate = statusCodePredicate;
		return this;
	}
	
	/**
	 * Waits for the given path.
	 *
	 * @param path the path to check
	 * @return this
	 */
	public HttpWaitAbortableStrategy forPath(final String path)
	{
		this.path = path;
		return this.self();
	}
	
	/**
	 * Wait for the given port.
	 *
	 * @param port the given port
	 * @return this
	 */
	public HttpWaitAbortableStrategy forPort(final int port)
	{
		this.livenessPort = Optional.of(port);
		return this.self();
	}
	
	/**
	 * Indicates that the status check should use HTTPS.
	 *
	 * @return this
	 */
	public HttpWaitAbortableStrategy usingTls()
	{
		this.tlsEnabled = true;
		return this.self();
	}
	
	/**
	 * Indicates the HTTP method to use (<code>GET</code> by default).
	 *
	 * @param method the HTTP method.
	 * @return this
	 */
	public HttpWaitAbortableStrategy withMethod(final String method)
	{
		this.method = method;
		return this.self();
	}
	
	/**
	 * Indicates that HTTPS connection could use untrusted (self signed) certificate chains.
	 *
	 * @return this
	 */
	public HttpWaitAbortableStrategy allowInsecure()
	{
		this.allowInsecure = true;
		return this.self();
	}
	
	/**
	 * Authenticate with HTTP Basic Authorization credentials.
	 *
	 * @param username the username
	 * @param password the password
	 * @return this
	 */
	public HttpWaitAbortableStrategy withBasicCredentials(
		final String username,
		final String password)
	{
		this.username = username;
		this.password = password;
		return this.self();
	}
	
	/**
	 * Add a custom HTTP Header to the call.
	 *
	 * @param name  The HTTP Header name
	 * @param value The HTTP Header value
	 * @return this
	 */
	public HttpWaitAbortableStrategy withHeader(
		final String name,
		final String value)
	{
		this.headers.put(name, value);
		return this.self();
	}
	
	/**
	 * Add multiple custom HTTP Headers to the call.
	 *
	 * @param headers Headers map of name/value
	 * @return this
	 */
	public HttpWaitAbortableStrategy withHeaders(final Map<String, String> headers)
	{
		this.headers.putAll(headers);
		return this.self();
	}
	
	/**
	 * Set the HTTP connections read timeout.
	 *
	 * @param timeout the timeout (minimum 1 millisecond)
	 * @return this
	 */
	public HttpWaitAbortableStrategy withReadTimeout(final Duration timeout)
	{
		if(timeout.toMillis() < 1)
		{
			throw new IllegalArgumentException("you cannot specify a value smaller than 1 ms");
		}
		this.readTimeout = timeout;
		return this.self();
	}
	
	/**
	 * Waits for the response to pass the given predicate
	 *
	 * @param responsePredicate The predicate to test the response against
	 * @return this
	 */
	public HttpWaitAbortableStrategy forResponsePredicate(final Predicate<String> responsePredicate)
	{
		this.responsePredicate = responsePredicate;
		return this.self();
	}
	
	// endregion
	
	@Override
	protected void waitUntilReady(final AbortMonitor abortMonitor)
	{
		final String containerName = this.waitStrategyTarget.getContainerInfo().getName();
		
		final Integer livenessCheckPort = this.livenessPort
			.map(this.waitStrategyTarget::getMappedPort)
			.orElseGet(() -> {
				final Set<Integer> livenessCheckPorts = this.getLivenessCheckPorts();
				if(livenessCheckPorts == null || livenessCheckPorts.isEmpty())
				{
					LOG.warn("{}: No exposed ports or mapped ports - cannot wait for status", containerName);
					return -1;
				}
				return livenessCheckPorts.iterator().next();
			});
		
		abortMonitor.throwIfRequired();
		
		if(null == livenessCheckPort || -1 == livenessCheckPort)
		{
			return;
		}
		final URI rawUri = this.buildLivenessUri(livenessCheckPort);
		final String uri = rawUri.toString();
		
		try
		{
			// Un-map the port for logging
			final int originalPort = this.waitStrategyTarget
				.getExposedPorts()
				.stream()
				.filter(exposedPort -> rawUri.getPort() == this.waitStrategyTarget.getMappedPort(exposedPort))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Target port " + rawUri.getPort() + " is not exposed"));
			LOG.info(
				"{}: Waiting for {} seconds for URL: {} (where port {} maps to container port {})",
				containerName,
				this.startupTimeout.getSeconds(),
				uri,
				rawUri.getPort(),
				originalPort
			);
		}
		catch(final RuntimeException e)
		{
			// do not allow a failure in logging to prevent progress, but log for diagnosis
			LOG.warn("Unexpected error occurred - will proceed to try to wait anyway", e);
		}
		
		abortMonitor.throwIfRequired();
		
		// try to connect to the URL
		try
		{
			this.startupRetryUntilSuccessWithRateLimitWhenNotAborted(
				abortMonitor,
				() -> this.connectAndCheck(uri));
			abortMonitor.throwIfRequired();
		}
		catch(final TimeoutException e)
		{
			throw new ContainerLaunchException(
				String.format(
					"Timed out waiting for URL to be accessible (%s should return HTTP %s)",
					uri,
					this.statusCodes.isEmpty() ? HttpURLConnection.HTTP_OK : this.statusCodes
				),
				e
			);
		}
	}
	
	protected void connectAndCheck(final String uri)
	{
		try
		{
			final HttpURLConnection connection = this.openConnection(uri);
			connection.setReadTimeout(Math.toIntExact(this.readTimeout.toMillis()));
			
			// authenticate
			if(this.username != null && !this.username.isEmpty())
			{
				connection.setRequestProperty(
					HEADER_AUTHORIZATION,
					this.buildAuthString(this.username, this.password)
				);
				connection.setUseCaches(false);
			}
			
			// Add user configured headers
			this.headers.forEach(connection::setRequestProperty);
			connection.setRequestMethod(this.method);
			connection.connect();
			
			LOG.trace("Get response code {}", connection.getResponseCode());
			
			// Choose the statusCodePredicate strategy depending on what we defined.
			if(!this.determineStatusCodePredicate().test(connection.getResponseCode()))
			{
				throw new IllegalStateException("HTTP response code was: " + connection.getResponseCode());
			}
			
			if(this.responsePredicate != null)
			{
				final String responseBody = this.getResponseBody(connection);
				
				LOG.trace("Got response {}", responseBody);
				
				if(!this.responsePredicate.test(responseBody))
				{
					throw new IllegalStateException("Response: " + responseBody + " did not match predicate");
				}
			}
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
	
	private Predicate<Integer> determineStatusCodePredicate()
	{
		if(this.statusCodes.isEmpty() && this.statusCodePredicate == null)
		{
			// We have no status code and no predicate so we expect a 200 OK response code
			return responseCode -> HttpURLConnection.HTTP_OK == responseCode;
		}
		else if(!this.statusCodes.isEmpty() && this.statusCodePredicate == null)
		{
			// We use the default status predicate checker when we only have status codes
			return this.statusCodes::contains;
		}
		else if(this.statusCodes.isEmpty())
		{
			// We only have a predicate
			return this.statusCodePredicate;
		}
		
		// We have both predicate and status code
		return this.statusCodePredicate.or(this.statusCodes::contains);
	}
	
	protected HttpURLConnection openConnection(final String uri) throws IOException
	{
		if(!this.tlsEnabled)
		{
			return (HttpURLConnection)new URL(uri).openConnection();
		}
		
		final HttpsURLConnection connection = (HttpsURLConnection)new URL(uri).openConnection();
		if(this.allowInsecure)
		{
			// Create a trust manager that does not validate certificate chains
			// and trust all certificates
			final TrustManager[] trustAllCerts = new TrustManager[]{
				new X509ExtendedTrustManager()
				{
					@Override
					public X509Certificate[] getAcceptedIssuers()
					{
						return new X509Certificate[0];
					}
					
					@Override
					public void checkClientTrusted(final X509Certificate[] certs, final String authType)
					{
					}
					
					@Override
					public void checkServerTrusted(final X509Certificate[] certs, final String authType)
					{
					}
					
					@Override
					public void checkClientTrusted(
						final X509Certificate[] chain,
						final String authType,
						final Socket socket)
					{
					}
					
					@Override
					public void checkServerTrusted(
						final X509Certificate[] chain,
						final String authType,
						final Socket socket)
					{
					}
					
					@Override
					public void checkClientTrusted(
						final X509Certificate[] chain,
						final String authType,
						final SSLEngine engine)
					{
					}
					
					@Override
					public void checkServerTrusted(
						final X509Certificate[] chain,
						final String authType,
						final SSLEngine engine)
					{
					}
				},
			};
			
			try
			{
				// Create custom SSL context and set the "trust all certificates" trust manager
				final SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(new KeyManager[0], trustAllCerts, new SecureRandom());
				connection.setSSLSocketFactory(sc.getSocketFactory());
			}
			catch(final NoSuchAlgorithmException | KeyManagementException ex)
			{
				throw new IOException("Unable to create custom SSL factory instance", ex);
			}
		}
		
		return connection;
	}
	
	/**
	 * Build the URI on which to check if the container is ready.
	 *
	 * @param livenessCheckPort the liveness port
	 * @return the liveness URI
	 */
	@SuppressWarnings("checkstyle:MagicNumber")
	protected URI buildLivenessUri(final int livenessCheckPort)
	{
		final String scheme = (this.tlsEnabled ? "https" : "http") + "://";
		final String host = this.waitStrategyTarget.getHost();
		
		final String portSuffix =
			this.tlsEnabled && livenessCheckPort == 443
				|| !this.tlsEnabled && livenessCheckPort == 80
				? ""
				: ":" + livenessCheckPort;
		
		return URI.create(scheme + host + portSuffix + this.path);
	}
	
	/**
	 * @param username the username
	 * @param password the password
	 * @return a basic authentication string for the given credentials
	 */
	protected String buildAuthString(final String username, final String password)
	{
		return AUTH_BASIC + BaseEncoding.base64().encode((username + ":" + password).getBytes());
	}
	
	@SuppressWarnings({"PMD.AvoidStringBuilderOrBuffer", "checkstyle:MagicNumber"})
	protected String getResponseBody(final HttpURLConnection connection) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
			connection.getResponseCode() >= 200 && connection.getResponseCode() < 300
				? connection.getInputStream()
				: connection.getErrorStream()));
		
		final StringBuilder builder = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null)
		{
			builder.append(line);
		}
		return builder.toString();
	}
}
