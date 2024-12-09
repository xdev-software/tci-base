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
package software.xdev.tci.factory.prestart.loadbalancing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;

import software.xdev.tci.safestart.SafeNamedContainerStarter;


/**
 * Default implementation of {@link LoadMonitor} using {@link NodeExporterContainer}.
 */
public class DefaultDockerLoadMonitor implements AutoCloseable, LoadMonitor
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultDockerLoadMonitor.class);
	
	protected final NodeExporterContainer nodeExporterContainer;
	
	protected final ScheduledExecutorService scrapeExecutor;
	protected final HttpClient httpClient;
	
	protected ScrapeData scrapeData;
	protected OptionalDouble idlePercent = OptionalDouble.empty(); // Idle load in percent. 12.34=12.34%; 0-100
	
	@SuppressWarnings("java:S2095")
	public DefaultDockerLoadMonitor()
	{
		this.nodeExporterContainer = new NodeExporterContainer()
			.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("container.node_exporter")))
			// We only need specific metrics so disable the rest
			.withCommand("--collector.disable-defaults --collector.cpu");
		new SafeNamedContainerStarter<>("load-monitor", this.nodeExporterContainer).start();
		
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();
		
		this.scrapeExecutor = Executors.newScheduledThreadPool(1, r ->
		{
			final Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("DockerLoadMonitor");
			return t;
		});
		this.scrape();
		this.scrapeExecutor.scheduleAtFixedRate(this::scrape, 0, 1, TimeUnit.SECONDS);
	}
	
	public void scrape()
	{
		try
		{
			final HttpResponse<String> response = this.httpClient.send(
				HttpRequest.newBuilder(URI.create(this.nodeExporterContainer.getExternalMetricsEndpoint()))
					.timeout(Duration.ofSeconds(5))
					.GET()
					.build(), HttpResponse.BodyHandlers.ofString());
			if(response.statusCode() != HttpStatus.SC_OK)
			{
				throw new IllegalStateException("Invalid response: " + response);
			}
			
			final long scrapeTime = System.currentTimeMillis();
			
			// Metrics look like this:
			// node_cpu_seconds_total{cpu="10",mode="idle"} 2012.73
			final Map<Integer, Double> cpuIdleSec = Stream.of(response.body().split("\n"))
				.filter(s -> s.startsWith("node_cpu_seconds_total"))
				.filter(s -> s.contains("mode=\"idle\""))
				.map(s -> s.split(" "))
				.filter(parts -> parts.length == 2)
				.map(parts -> {
					final String startCPU = "cpu=\"";
					final String cpu = parts[0].substring(parts[0].indexOf(startCPU) + startCPU.length());
					final int cpuIndex = Integer.parseInt(cpu.substring(0, cpu.indexOf('\"')));
					final double idleSec = Double.parseDouble(parts[1]);
					
					return Map.entry(cpuIndex, idleSec);
				})
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			
			final ScrapeData newScrapeData = new ScrapeData(scrapeTime, cpuIdleSec);
			this.calculateScrapeDataDiff(this.scrapeData, newScrapeData);
			this.scrapeData = newScrapeData;
		}
		catch(final InterruptedException iex)
		{
			LOG.warn("Got interrupted", iex);
			Thread.currentThread().interrupt();
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to scrape", ex);
		}
	}
	
	protected void calculateScrapeDataDiff(final ScrapeData oldScrapeData, final ScrapeData newScrapeData)
	{
		if(oldScrapeData == null)
		{
			return;
		}
		
		final long diffMs = newScrapeData.scrapeTime() - oldScrapeData.scrapeTime();
		if(diffMs <= 0)
		{
			// Something is wrong
			return;
		}
		
		double totalCoreIdleSec = 0;
		for(final int cpuIndex : newScrapeData.coreIdleSec().keySet())
		{
			final double newCoreSec = newScrapeData.coreIdleSec().get(cpuIndex);
			final double oldCoreSec = oldScrapeData.coreIdleSec().getOrDefault(cpuIndex, newCoreSec);
			
			totalCoreIdleSec += (newCoreSec - oldCoreSec);
		}
		
		final double avgCoreIdleMs = totalCoreIdleSec / newScrapeData.coreIdleSec().size() * 1000;
		this.idlePercent = OptionalDouble.of(Math.max(0, Math.min(1, avgCoreIdleMs / diffMs)) * 100);
		LOG.debug("IDLE {}%", this.idlePercent);
	}
	
	@Override
	public OptionalDouble getCurrentIdlePercent()
	{
		return this.idlePercent;
	}
	
	@Override
	public void close()
	{
		if(!this.scrapeExecutor.isShutdown())
		{
			this.scrapeExecutor.shutdown();
		}
		
		// Shutdown is only supported in Java 21+
		try
		{
			final Method mClose = HttpClient.class.getDeclaredMethod("close");
			mClose.invoke(this.httpClient);
		}
		catch(final NoSuchMethodException | IllegalAccessException | InvocationTargetException ex)
		{
			LOG.debug("Unable to close HttpClient. Likely running on Java < 21 where method does not exist", ex);
		}
		
		this.nodeExporterContainer.stop();
	}
	
	protected record ScrapeData(long scrapeTime, Map<Integer, Double> coreIdleSec)
	{
	}
}
