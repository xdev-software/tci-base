package software.xdev.tci.demo;

import java.util.Optional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@SpringBootApplication
public class Application
{
	public static void main(final String[] args)
	{
		System.setProperty(
			"spring.config.additional-location",
			"optional:"
				+ "classpath:/application-add.yml,"
				+ "classpath:/application-add-log.yml"
				+ Optional.ofNullable(System.getProperty("spring.config.additional-location"))
				.map(s -> "," + s)
				.orElse(""));
		SpringApplication.run(Application.class, args);
	}
}
