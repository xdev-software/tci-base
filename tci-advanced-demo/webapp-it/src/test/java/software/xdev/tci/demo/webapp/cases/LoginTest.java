package software.xdev.tci.demo.webapp.cases;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import software.xdev.tci.demo.tci.selenium.TestBrowser;
import software.xdev.tci.demo.webapp.base.InfraPerCaseTest;


class LoginTest extends InfraPerCaseTest
{
	@DisplayName("Check Login and Logout")
	@ParameterizedTest
	@EnumSource(TestBrowser.class)
	void checkLoginAndLogout(final TestBrowser browser)
	{
		this.startAll(browser);
		
		Assertions.assertDoesNotThrow(() ->
		{
			this.loginAndGotoMainSite();
			this.logout();
		});
	}
	
	protected static CloseableHttpClient createDefaultHttpClient()
	{
		return createDefaultHttpClient(false);
	}
	
	protected static CloseableHttpClient createDefaultHttpClient(final boolean notFollowRedirects)
	{
		final Duration timeout = Duration.ofSeconds(30);
		final HttpClientBuilder builder = HttpClientBuilder.create()
			.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(ConnectionConfig.custom()
					.setConnectTimeout(Timeout.of(timeout))
					.setSocketTimeout(Timeout.of(timeout))
					.build())
				.build());
		if(notFollowRedirects)
		{
			builder.disableRedirectHandling();
		}
		return builder.build();
	}
	
	private Stream<Executable> assertsNoSessionNoLoginAndCode(final int expectedCode, final HttpResponse response)
	{
		return Stream.of(
			() -> assertEquals(expectedCode, response.getCode()),
			() -> assertNull(response.getHeader("Set-Cookie"))
		);
	}
	
	@DisplayName("No session should be created for public static resource")
	@Test
	void checkNoSessionCreatedForPublicStaticResource() throws IOException
	{
		this.startBaseInfrastructure(null);
		try(final CloseableHttpClient client = createDefaultHttpClient())
		{
			final HttpGet httpGet = new HttpGet(this.appInfra().getExternalHTTPEndpoint() + "/robots.txt");
			try(final ClassicHttpResponse response = client.execute(httpGet, r -> r))
			{
				assertAll(this.assertsNoSessionNoLoginAndCode(HttpStatus.SC_OK, response));
			}
		}
	}
	
	@DisplayName("No session should be created for actuator")
	@ParameterizedTest(name = " {displayName} [withAuth={0}, existingPath={1}] expect={2}")
	@MethodSource
	void checkNoSessionCreatedForActuator(final boolean withAuth, final boolean existingPath, final int expectedCode)
		throws IOException
	{
		this.startBaseInfrastructure(null);
		// If no auth -> do not follow the redirects since they point to the not externally reachable OIDC server
		try(final CloseableHttpClient client = createDefaultHttpClient(!withAuth))
		{
			final HttpGet httpGet = new HttpGet(
				this.appInfra().getExternalHTTPEndpoint() + "/actuator" + (existingPath ? "" : "/abc"));
			if(withAuth)
			{
				final String auth =
					this.appInfra().getActuatorUsername() + ":" + this.appInfra().getActuatorPassword();
				httpGet.setHeader(
					HttpHeaders.AUTHORIZATION,
					"Basic " + new String(Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1))));
			}
			try(final ClassicHttpResponse response = client.execute(httpGet, r -> r))
			{
				assertAll(this.assertsNoSessionNoLoginAndCode(expectedCode, response));
			}
		}
	}
	
	static Stream<Arguments> checkNoSessionCreatedForActuator()
	{
		return Stream.of(
			Arguments.of(false, true, HttpStatus.SC_MOVED_TEMPORARILY),
			Arguments.of(true, true, HttpStatus.SC_OK),
			Arguments.of(true, false, HttpStatus.SC_NOT_FOUND),
			Arguments.of(false, false, HttpStatus.SC_MOVED_TEMPORARILY)
		);
	}
}
