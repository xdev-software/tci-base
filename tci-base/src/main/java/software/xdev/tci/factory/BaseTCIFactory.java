package software.xdev.tci.factory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import software.xdev.tci.TCI;
import software.xdev.tci.tracing.TCITracer;


@SuppressWarnings("java:S119")
public abstract class BaseTCIFactory<
	C extends GenericContainer<C>,
	I extends TCI<C>>
	implements TCIFactory<C, I>
{
	private final Logger logger;
	protected Set<I> returnedAndInUse = Collections.synchronizedSet(new HashSet<>());
	protected boolean warmedUp;
	
	protected BiFunction<C, String, I> infraBuilder;
	protected final Supplier<C> containerBuilder;
	protected final String containerBaseName;
	protected final String containerLoggerName;
	protected final TCITracer tracer = new TCITracer();
	
	protected BaseTCIFactory(
		final BiFunction<C, String, I> infraBuilder,
		final Supplier<C> containerBuilder,
		final String containerBaseName,
		final String containerLoggerName)
	{
		this.infraBuilder = Objects.requireNonNull(infraBuilder);
		
		this.containerBuilder = Objects.requireNonNull(containerBuilder);
		this.containerBaseName = Objects.requireNonNull(containerBaseName);
		this.containerLoggerName = Objects.requireNonNull(containerLoggerName);
		
		this.logger = LoggerFactory.getLogger(this.getClass());
		
		this.register();
	}
	
	@Override
	public void warmUp()
	{
		if(!this.warmedUp)
		{
			this.warmUpSync();
		}
	}
	
	protected synchronized void warmUpSync()
	{
		if(this.warmedUp)
		{
			return;
		}
		
		final long startTime = System.currentTimeMillis();
		
		this.warmUpInternal();
		this.warmedUp = true;
		
		this.tracer.timedAdd("warmUp", System.currentTimeMillis() - startTime);
	}
	
	protected void warmUpInternal()
	{
		// No OP
	}
	
	protected C buildContainer()
	{
		return this.containerBuilder.get()
			.withLogConsumer(getLogConsumer(this.containerLoggerName));
	}
	
	protected I registerReturned(final I infra)
	{
		this.returnedAndInUse.add(infra);
		infra.setOnStopped(() -> this.returnedAndInUse.remove(infra));
		return infra;
	}
	
	@Override
	public void close()
	{
		// NO OP
	}
	
	@Override
	public Set<I> getReturnedAndInUse()
	{
		return new HashSet<>(this.returnedAndInUse);
	}
	
	@Override
	public TCITracer getTracer()
	{
		return this.tracer;
	}
	
	protected Logger log()
	{
		return this.logger;
	}
	
	protected static Slf4jLogConsumer getLogConsumer(final String name)
	{
		return new Slf4jLogConsumer(LoggerFactory.getLogger(name));
	}
}