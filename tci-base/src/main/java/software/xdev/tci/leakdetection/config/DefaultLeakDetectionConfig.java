package software.xdev.tci.leakdetection.config;

public class DefaultLeakDetectionConfig implements LeakDetectionConfig
{
	@Override
	public boolean enabled()
	{
		return true;
	}
}
