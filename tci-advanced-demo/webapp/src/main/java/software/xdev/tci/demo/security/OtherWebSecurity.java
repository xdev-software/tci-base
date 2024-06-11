package software.xdev.tci.demo.security;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import software.xdev.tci.demo.config.SystemConfig;
import software.xdev.tci.demo.metrics.SharedMetrics;
import software.xdev.tci.demo.util.SHA256Hashing;


@EnableWebSecurity
@Configuration
public class OtherWebSecurity
{
	static final Set<String> PUBLIC_STATIC_FILES = Set.of(
		"/robots.txt",
		"/favicon.ico",
		"/assets/**",
		"/lib/**");
	static final String ACTUATOR = "/actuator/**";
	
	private final Counter actuatorLoginSuccess;
	private final Counter actuatorLoginFailed;
	
	@Autowired
	SystemConfig config;
	
	public OtherWebSecurity(@Autowired final MeterRegistry registry)
	{
		final String actuatorLoginName = SharedMetrics.PREFIX + "actuator_login";
		this.actuatorLoginSuccess =
			registry.counter(actuatorLoginName, SharedMetrics.TAG_OUTCOME, "success");
		this.actuatorLoginFailed =
			registry.counter(actuatorLoginName, SharedMetrics.TAG_OUTCOME, "failed");
	}
	
	@Bean
	@Order(2)
	public SecurityFilterChain configureStaticResources(final HttpSecurity http) throws Exception
	{
		// Static resources that require no authentication
		return http
			.securityMatcher(PUBLIC_STATIC_FILES.toArray(String[]::new))
			.authorizeHttpRequests(a -> a.anyRequest().permitAll())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.build();
	}
	
	private static final String ROLE_ACTUATOR = "ACTUATOR";
	
	@Bean
	@Order(1)
	public SecurityFilterChain configureActuator(final HttpSecurity http) throws Exception
	{
		// Actuator endpoint
		return http
			.securityMatcher(ACTUATOR)
			.authorizeHttpRequests(a -> a.anyRequest().hasRole(ROLE_ACTUATOR))
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(Customizer.withDefaults())
			.authenticationManager(new ProviderManager(this.getActuatorAuthProvider()))
			.build();
	}
	
	private AuthenticationProvider getActuatorAuthProvider()
	{
		/*
		 * (20.10.2021 AB)NOTE:
		 * This is specially configured so that the server doesn't have the password in plain text.
		 * There are also quick exits inside to make the code work faster.
		 */
		final String pwHash = this.config.getActuator().getPasswordHash();
		
		final DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setUserDetailsService(new InMemoryUserDetailsManager(User.builder()
			.username(this.config.getActuator().getUsername())
			.password(pwHash)
			.roles(ROLE_ACTUATOR)
			.build()));
		daoAuthenticationProvider.setPasswordEncoder(new PasswordEncoder()
		{
			@Override
			public boolean matches(final CharSequence rawPassword, final String encodedPassword)
			{
				if(rawPassword == null || rawPassword.isEmpty() || !pwHash.equals(encodedPassword))
				{
					OtherWebSecurity.this.actuatorLoginFailed.increment();
					return false;
				}
				
				final boolean success = SHA256Hashing.hash(rawPassword.toString()).equals(encodedPassword);
				if(success)
				{
					OtherWebSecurity.this.actuatorLoginSuccess.increment();
				}
				else
				{
					OtherWebSecurity.this.actuatorLoginFailed.increment();
				}
				
				return success;
			}
			
			@Override
			public String encode(final CharSequence rawPassword)
			{
				return rawPassword.toString();
			}
		});
		
		return daoAuthenticationProvider;
	}
}
