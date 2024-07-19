package software.xdev.tci.demo.config;

import jakarta.validation.constraints.NotNull;

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
	
	public ActuatorConfig getActuator()
	{
		return this.actuator;
	}
	
	public void setActuator(final ActuatorConfig actuator)
	{
		this.actuator = actuator;
	}
	
	@Override
	public String toString()
	{
		return "SystemConfig [actuator="
			+ this.actuator
			+ "]";
	}
}
