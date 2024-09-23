package software.xdev.tci.demo.security;

import static java.util.Map.entry;
import static software.xdev.tci.demo.security.CSP.POLICY_NONE;
import static software.xdev.tci.demo.security.CSP.POLICY_SELF;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;

import software.xdev.spring.security.web.authentication.ui.advanced.AdvancedLoginPageAdapter;
import software.xdev.spring.security.web.authentication.ui.advanced.config.AdditionalOAuth2ClientProperties;


@EnableWebSecurity
@Configuration
@EnableConfigurationProperties(AdditionalOAuth2ClientProperties.class)
public class MainWebSecurity
{
	@SuppressWarnings("java:S4502") // See below
	@Bean(name = "mainSecurityFilterChainBean")
	public SecurityFilterChain configure(
		final HttpSecurity http,
		final AdditionalOAuth2ClientProperties additionalOAuth2ClientProperties) throws Exception
	{
		http.with(
				new AdvancedLoginPageAdapter<>(http),
				c -> c
					.customizePages(p -> p.setHeaderElements(List.of(
						"<link href=\"/lib/bootstrap-5.3.3.min.css\" rel=\"stylesheet\"/>",
						"<link href=\"/lib/theme.css\" rel=\"stylesheet\"/>",
						"<script src=\"/lib/bootstrap-5.3.3.bundle.min.js\"></script>",
						"<script src=\"/lib/theme.js\"></script>"
					)))
					.customizeLoginPage(p -> p
						.additionalOAuth2RegistrationProperties(additionalOAuth2ClientProperties.getRegistration())
						.header("<div class='d-flex justify-content-center'>"
							+ "  <img src='/assets/XDEV_LOGO.svg' alt='XDEV' style='max-width:100%;"
							+ "height:calc(var(--bs-body-font-size) * 2.5)'></img>"
							+ "</div>"
							+ "<h2 class='h2 mb-3 text-center'>Demo</h2>")
					))
			.headers(h -> h
				.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
				// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options
				.contentTypeOptions(Customizer.withDefaults())
				.contentSecurityPolicy(csp -> csp.policyDirectives(this.getCSP())))
			.sessionManagement(c ->
				// Limit maximum session per user
				c.sessionConcurrency(sc -> sc.maximumSessions(5)))
			.oauth2Login(c -> c.defaultSuccessUrl("/"))
			// Disable CSRF for REST API for demo purposes
			.csrf(c -> c.ignoringRequestMatchers("/api/**"))
			.authorizeHttpRequests(urlRegistry -> urlRegistry.anyRequest().authenticated())
			.logout(Customizer.withDefaults())
			// nothing needs to be saved
			.requestCache(r -> r.requestCache(new NullRequestCache()));
		
		return http.build();
	}
	
	protected String getCSP()
	{
		return CSP.build(Map.ofEntries(
			entry(
				"default-src",
				POLICY_SELF
					+ (this.isDevMode()
					// Allow ws://locahost:* in Demo mode for SpringbootDevTools
					? " ws://localhost:*"
					: "")),
			entry("script-src", POLICY_SELF + " 'unsafe-inline'"),
			entry("style-src", POLICY_SELF + " 'unsafe-inline'"),
			entry("font-src", POLICY_SELF),
			entry("img-src", POLICY_SELF + " data:"),
			// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/object-src
			// https://csp.withgoogle.com/docs/strict-csp.html
			entry("object-src", POLICY_NONE),
			// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/base-uri
			entry("base-uri", POLICY_SELF),
			// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/form-action
			// When using 'self':
			// * Webkit based Browsers have problems here: https://github.com/w3c/webappsec-csp/issues/8
			// * Firefox is
			// As of 2024-03 CSP3 added 'unsafe-allow-redirects' however it's not implemented by any browser yet
			// Fallback for now '*'
			entry("form-action", "*"),
			// Replaces X-Frame-Options
			entry("frame-src", POLICY_SELF),
			entry("frame-ancestors", POLICY_SELF)));
	}
	
	protected boolean isDevMode()
	{
		try
		{
			Class.forName("org.springframework.boot.devtools.settings.DevToolsSettings");
			return true;
		}
		catch(final ClassNotFoundException nf)
		{
			return false;
		}
	}
}
