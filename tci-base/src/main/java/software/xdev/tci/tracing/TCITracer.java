package software.xdev.tci.tracing;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class TCITracer
{
	private final Map<String, Timed> timers = Collections.synchronizedMap(new HashMap<>());
	
	public Timed getTimedOrCreate(final String name)
	{
		return this.timers.computeIfAbsent(name, x -> new Timed());
	}
	
	public void timedAdd(final String name, final long ms)
	{
		this.getTimedOrCreate(name).addMs(ms);
	}
	
	public Map<String, Timed> getTimers()
	{
		return this.timers;
	}
	
	public static class Timed
	{
		private long countCalled;
		private long totalMs;
		
		public synchronized void addMs(final long ms)
		{
			this.countCalled++;
			this.totalMs += ms;
		}
		
		public long getCountCalled()
		{
			return this.countCalled;
		}
		
		public long getTotalMs()
		{
			return this.totalMs;
		}
		
		public double getAverageMs()
		{
			if(this.countCalled == 0)
			{
				return 0;
			}
			return this.totalMs / (double)this.countCalled;
		}
		
		public long getAverageMsRounded()
		{
			return Math.round(this.getAverageMs());
		}
		
		@Override
		public String toString()
		{
			return formatTimed(this);
		}
		
		public static String formatTimed(final Timed timed)
		{
			return prettyPrintMS(timed.getAverageMsRounded())
				+ " / "
				+ timed.getCountCalled()
				+ " / "
				+ prettyPrintMS(timed.getTotalMs());
		}
		
		public static String prettyPrintMS(final long ms)
		{
			if(ms < 1000)
			{
				return ms + "ms";
			}
			// https://stackoverflow.com/a/40487511
			return Duration.ofMillis(ms).toString()
				.substring(2)
				.replaceAll("(\\d[HMS])(?!$)", "$1 ")
				.toLowerCase();
		}
	}
}
