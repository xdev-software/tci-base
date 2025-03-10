package software.xdev.tci.demo.init;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import software.xdev.tci.demo.config.SystemConfig;


@Component
public class StartUpConfigLogger
{
	public StartUpConfigLogger(final SystemConfig config)
	{
		LoggerFactory.getLogger(this.getClass()).info("Loaded config: {}", config);
	}
}
