package software.xdev.tci.portfixation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;


/**
 * Utility class for getting a random port using a dummy Docker container.
 * <p/>
 * Created as a workaround for <a href="https://github.com/moby/moby/issues/44137">moby/moby#44137</a>
 */
public final class PortFixation
{
	private PortFixation()
	{
	}
	
	private static TriConsumer<GenericContainer<?>, Integer, Integer> addFixedExposedPortFunc;
	
	public static void makeExposedPortsFix(final GenericContainer<?> container)
	{
		// Cache
		if(addFixedExposedPortFunc == null)
		{
			initAddFixedExposedPortFunc();
		}
		
		Stream.concat(
				container.getExposedPorts().stream(),
				Optional.of(container)
					.filter(AdditionalPortsForFixedExposingContainer.class::isInstance)
					.map(AdditionalPortsForFixedExposingContainer.class::cast)
					.map(AdditionalPortsForFixedExposingContainer::getAdditionalPortsForFixedExposing)
					.stream()
					.flatMap(Collection::stream))
			.distinct()
			.map(cPort ->
				CompletableFuture.runAsync(() ->
					addFixedExposedPortFunc.accept(container, PortFixation.getRandomFreePort(), cPort)))
			.toList()
			.forEach(CompletableFuture::join);
	}
	
	@SuppressWarnings("java:S3011")
	private static synchronized void initAddFixedExposedPortFunc()
	{
		if(addFixedExposedPortFunc != null)
		{
			return;
		}
		try
		{
			final Method mAddFixedExposedPort =
				GenericContainer.class.getDeclaredMethod("addFixedExposedPort", int.class, int.class);
			mAddFixedExposedPort.setAccessible(true);
			addFixedExposedPortFunc = (container, hostPort, containerPort) -> {
				try
				{
					mAddFixedExposedPort.invoke(container, hostPort, containerPort);
				}
				catch(final IllegalAccessException | InvocationTargetException e)
				{
					throw new IllegalStateException("Unable to invoke", e);
				}
			};
		}
		catch(final NoSuchMethodException e)
		{
			throw new IllegalStateException("Unable to find underlying method", e);
		}
	}
	
	private static int getRandomFreePort()
	{
		try(final GetPortContainer container = new GetPortContainer())
		{
			container.start();
			return container.getPort();
		}
	}
	
	static class GetPortContainer extends GenericContainer<GetPortContainer>
	{
		protected static final DockerImageName IMAGE = DockerImageName.parse("alpine:3");
		protected static final int PORT = 5000;
		
		public GetPortContainer()
		{
			super(IMAGE);
			// Create a netcat server listener so that the startup check succeeds
			this.setCommand("/bin/sh", "-c", "while true; do nc -v -lk -p " + PORT + "; done");
			this.addExposedPort(PORT);
		}
		
		public Integer getPort()
		{
			return this.getMappedPort(PORT);
		}
		
		@Override
		protected Logger logger()
		{
			return DockerLoggerFactory.getLogger("container.getport");
		}
	}
	
	
	@FunctionalInterface
	interface TriConsumer<T, U, V>
	{
		void accept(T k, U v, V s);
	}
}
