package software.xdev.tci.demo.persistence.config;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SuppressWarnings("java:S1118") // This is not a utility class
@EnableJpaRepositories("software.xdev.tci.demo.persistence.jpa.dao")
public abstract class DefaultJPAConfig
{
	public static final String ENTITY_PACKAGE = "software.xdev.tci.demo.entities";
}
