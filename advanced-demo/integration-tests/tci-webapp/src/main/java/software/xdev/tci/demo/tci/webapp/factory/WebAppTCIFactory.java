package software.xdev.tci.demo.tci.webapp.factory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import software.xdev.tci.concurrent.Suppliers;
import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.demo.tci.webapp.WebAppTCI;
import software.xdev.tci.demo.tci.webapp.containers.WebAppContainer;
import software.xdev.tci.demo.tci.webapp.containers.WebAppContainerBuilder;
import software.xdev.tci.envperf.EnvironmentPerformance;
import software.xdev.tci.factory.prestart.PreStartableTCIFactory;
import software.xdev.tci.misc.ContainerMemory;
import software.xdev.tci.pull.policy.NeverPullPolicy;


public class WebAppTCIFactory extends PreStartableTCIFactory<WebAppContainer, WebAppTCI>
{
	protected static final Supplier<String> IMAGE_NAME_SUPPLIER =
		Suppliers.memoize(WebAppContainerBuilder::getImageName);
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public WebAppTCIFactory(final Consumer<WebAppContainer> additionalContainerBuilder)
	{
		super(
			WebAppTCI::new,
			() -> {
				final WebAppContainer container = new WebAppContainer(IMAGE_NAME_SUPPLIER.get(), true)
					.withImagePullPolicy(NeverPullPolicy.INSTANCE)
					.withDefaultWaitStrategy(
						Duration.ofSeconds(40L + 20L * EnvironmentPerformance.cpuSlownessFactor()),
						WebAppTCI.ACTUATOR_USERNAME,
						WebAppTCI.ACTUATOR_PASSWORD)
					.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemory(ContainerMemory.M1G))
					// ACTUATOR
					.withActuator(
						WebAppTCI.ACTUATOR_USERNAME,
						// PW = admin in SHA256
						"8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918")
					// Configure for Tests
					.withDisableHTTPS();
				additionalContainerBuilder.accept(container);
				return container;
			},
			"webapp",
			"container.webapp",
			"WebApp");
	}
	
	@Override
	protected void warmUpInternal()
	{
		CompletableFuture.runAsync(IMAGE_NAME_SUPPLIER::get, TCIExecutorServiceHolder.instance());
		super.warmUpInternal();
	}
}
