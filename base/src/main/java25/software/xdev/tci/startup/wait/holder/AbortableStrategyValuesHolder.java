package software.xdev.tci.startup.wait.holder;


public final class AbortableStrategyValuesHolder
{
	private static final ScopedValue<AbortableStrategyValues> SV = ScopedValue.newInstance();
	
	public static void executeWith(final Runnable runnable, final AbortableStrategyValues values)
	{
		ScopedValue.where(SV, values).run(runnable);
	}
	
	public static AbortableStrategyValues get()
	{
		return SV.get();
	}
	
	private AbortableStrategyValuesHolder()
	{
	}
}
