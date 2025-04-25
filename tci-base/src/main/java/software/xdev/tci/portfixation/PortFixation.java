/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.tci.portfixation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;


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
	
	static boolean initializedReflectFuncs;
	static Function<GenericContainer<?>, Set<ExposedPort>> exposedPortAccess;
	static TriConsumer<GenericContainer<?>, Integer, ExposedPort> addFixedExposedPortFunc;
	
	public static void makeExposedPortsFix(final GenericContainer<?> container)
	{
		// Cache
		if(!initializedReflectFuncs)
		{
			initReflectFuncs();
		}
		
		final List<ExposedPort> requiredPorts = Stream.concat(
				exposedPortAccess.apply(container).stream(),
				Optional.of(container)
					.filter(AdditionalPortsForFixedExposingContainer.class::isInstance)
					.map(AdditionalPortsForFixedExposingContainer.class::cast)
					.map(AdditionalPortsForFixedExposingContainer::getAdditionalPortsForFixedExposing)
					.stream()
					.flatMap(Collection::stream))
			.distinct()
			.toList();
		
		if(requiredPorts.isEmpty())
		{
			return;
		}
		
		final Map<InternetProtocol, List<ExposedPort>> protocolPorts =
			requiredPorts.stream().collect(Collectors.groupingBy(ExposedPort::getProtocol));
		
		final Map<InternetProtocol, List<Integer>> randomHostFreePorts =
			PortFixation.getRandomFreePorts(protocolPorts.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
		
		protocolPorts.forEach((proto, containerPorts) -> {
			final List<Integer> protoRandomHostFreePort = randomHostFreePorts.get(proto);
			IntStream.range(0, containerPorts.size())
				.forEach(i -> addFixedExposedPortFunc.accept(
					container,
					protoRandomHostFreePort.get(i),
					containerPorts.get(i)));
		});
	}
	
	@SuppressWarnings({"java:S3011", "unchecked"})
	static synchronized void initReflectFuncs()
	{
		if(initializedReflectFuncs)
		{
			return;
		}
		
		try
		{
			
			final Method mGetContainerDef = GenericContainer.class.getDeclaredMethod("getContainerDef");
			mGetContainerDef.setAccessible(true);
			
			final Class<?> containerDefClazz = Class.forName("org.testcontainers.containers.ContainerDef");
			
			final Field fExposedPorts = containerDefClazz.getDeclaredField("exposedPorts");
			fExposedPorts.setAccessible(true);
			
			exposedPortAccess = c ->
			{
				try
				{
					return (Set<ExposedPort>)fExposedPorts.get(mGetContainerDef.invoke(c));
				}
				catch(final IllegalAccessException | InvocationTargetException e)
				{
					throw new IllegalStateException("Unable to invoke", e);
				}
			};
			
			// Can't use GenericContainer#addFixedExposedPort because there is another InternetProtocol-class used!
			final Method mAddPortBinding = containerDefClazz.getDeclaredMethod("addPortBinding", PortBinding.class);
			mAddPortBinding.setAccessible(true);
			
			addFixedExposedPortFunc = (container, hostPort, containerPort) -> {
				final PortBinding portBinding = new PortBinding(Ports.Binding.bindPort(hostPort), containerPort);
				try
				{
					mAddPortBinding.invoke(mGetContainerDef.invoke(container), portBinding);
				}
				catch(final IllegalAccessException | InvocationTargetException e)
				{
					throw new IllegalStateException("Unable to invoke", e);
				}
			};
			
			initializedReflectFuncs = true;
		}
		catch(final Exception e)
		{
			throw new IllegalStateException("Unable to init reflective functions", e);
		}
	}
	
	private static Map<InternetProtocol, List<Integer>> getRandomFreePorts(
		final Map<InternetProtocol, Integer> protocolAmounts)
	{
		if(protocolAmounts.isEmpty())
		{
			return Map.of();
		}
		
		try(final GetPortContainer container = new GetPortContainer(protocolAmounts))
		{
			container.start();
			return container.getPorts();
		}
	}
	
	@SuppressWarnings("java:S2160") // Not needed
	static class GetPortContainer extends GenericContainer<GetPortContainer>
	{
		protected static final Logger LOG = DockerLoggerFactory.getLogger("container.getport");
		protected static final DockerImageName IMAGE = DockerImageName.parse("alpine:3");
		protected static final int BASE_PORT = 2000;
		
		protected final Map<InternetProtocol, Set<ExposedPort>> ports;
		
		public GetPortContainer(final Map<InternetProtocol, Integer> protocolAmounts)
		{
			super(IMAGE);
			// Create a netcat server listener so that the startup check succeeds
			this.setCommand("/bin/sh", "-c", "while true; do nc -v -lk -p " + BASE_PORT + "; done");
			
			final AtomicInteger portCounter = new AtomicInteger(BASE_PORT);
			this.ports = protocolAmounts.entrySet()
				.stream()
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					e -> IntStream.range(0, e.getValue())
						.mapToObj(counter -> new ExposedPort(portCounter.incrementAndGet(), e.getKey()))
						.collect(Collectors.toSet())));
			
			this.addExposedPort(BASE_PORT);
			
			this.ports.values().forEach(PortFixation.exposedPortAccess.apply(this)::addAll);
		}
		
		@Override
		public List<Integer> getExposedPorts()
		{
			return List.of(BASE_PORT); // Report only the base port as only this port listens
		}
		
		public Map<InternetProtocol, List<Integer>> getPorts()
		{
			return this.ports.entrySet().stream()
				.collect(Collectors.toMap(
					Map.Entry::getKey, e -> e.getValue()
						.stream()
						.map(ExposedPort::getPort)
						.map(this::getMappedPort)
						.distinct()
						.toList()
				));
		}
		
		@Override
		protected Logger logger()
		{
			return LOG;
		}
	}
	
	
	@FunctionalInterface
	interface TriConsumer<T, U, V>
	{
		void accept(T t, U u, V v);
	}
}
