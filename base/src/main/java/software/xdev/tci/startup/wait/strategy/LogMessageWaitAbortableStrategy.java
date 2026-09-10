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
package software.xdev.tci.startup.wait.strategy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import com.github.dockerjava.api.command.LogContainerCmd;

import software.xdev.tci.startup.wait.AbortMonitor;


/**
 * Based on {@link org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy}
 */
public class LogMessageWaitAbortableStrategy extends AbstractWaitAbortableStrategy<LogMessageWaitAbortableStrategy>
{
	protected String regEx;
	
	protected int times = 1;
	
	@Override
	protected void waitUntilReady(final AbortMonitor abortMonitor)
	{
		try(final LogContainerCmd cmd = this.waitStrategyTarget
			.getDockerClient()
			.logContainerCmd(this.waitStrategyTarget.getContainerId())
			.withFollowStream(true)
			.withSince(0)
			.withStdOut(true)
			.withStdErr(true))
		{
			try(final FrameConsumerResultCallback callback = new FrameConsumerResultCallback())
			{
				final WaitingConsumer waitingConsumer = new WaitingConsumer();
				callback.addConsumer(OutputFrame.OutputType.STDOUT, waitingConsumer);
				callback.addConsumer(OutputFrame.OutputType.STDERR, waitingConsumer);
				
				cmd.exec(callback);
				
				final Predicate<OutputFrame> waitPredicate = outputFrame -> {
					abortMonitor.throwIfRequired();
					
					// (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
					return outputFrame.getUtf8String().matches("(?s)" + this.regEx);
				};
				try
				{
					waitingConsumer.waitUntil(
						waitPredicate,
						this.startupTimeout.getSeconds(),
						TimeUnit.SECONDS,
						this.times);
				}
				catch(final TimeoutException e)
				{
					throw new ContainerLaunchException(
						"Timed out waiting for log output matching '" + this.regEx + "'",
						e);
				}
			}
			catch(final IOException ioe)
			{
				throw new UncheckedIOException(ioe);
			}
		}
	}
	
	public LogMessageWaitAbortableStrategy withRegEx(final String regEx)
	{
		this.regEx = regEx;
		return this.self();
	}
	
	public LogMessageWaitAbortableStrategy withTimes(final int times)
	{
		this.times = times;
		return this.self();
	}
}
