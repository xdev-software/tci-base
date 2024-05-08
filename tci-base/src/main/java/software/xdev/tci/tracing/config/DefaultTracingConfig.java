package software.xdev.tci.tracing.config;

public class DefaultTracingConfig implements TracingConfig
{
	@Override
	public boolean enabled()
	{
		return true;
	}
}
