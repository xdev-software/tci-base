package software.xdev.tci.demo.config;

import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Be careful with the formats:
// https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties.vs-value-annotation


@Configuration
@ConfigurationProperties(prefix = "demo")
public class SystemConfig
{
	@NotNull
	private ActuatorConfig actuator;
	
	/**
	 * Determines if the system uses https (e.g. behind a reverse proxy)
	 *
	 * @see <a href="https://www.baeldung.com/spring-security-session">
	 * https://www.baeldung.com/spring-security-session
	 * </a>
	 */
	@Value("${server.servlet.session.cookie.secure:true}")
	private boolean secure;
	
	public ActuatorConfig getActuator()
	{
		return this.actuator;
	}
	
	public void setActuator(final ActuatorConfig actuator)
	{
		this.actuator = actuator;
	}
	
	public boolean isSecure()
	{
		return this.secure;
	}
	
	public void setSecure(final boolean secure)
	{
		this.secure = secure;
	}
	
	@Override
	public String toString()
	{
		return "SystemConfig [actuator="
			+ this.actuator
			+ ", secure="
			+ this.secure
			+ "]";
	}
}
