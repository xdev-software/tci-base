/*
 * Copyright © 2025 XDEV Software (https://xdev.software)
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
package software.xdev.tci.selenium.testbase;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.lifecycle.TestDescription;

import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.selenium.BrowserTCI;


@SuppressWarnings("PMD.MoreThanOneLogger")
public class SeleniumRecorder
{
	private static final Logger INTERNAL_LOG = LoggerFactory.getLogger(SeleniumRecorder.class);
	
	protected final Logger logger;
	
	public SeleniumRecorder()
	{
		this(INTERNAL_LOG);
	}
	
	public SeleniumRecorder(final Logger logger)
	{
		this.logger = logger;
	}
	
	public CompletableFuture<Void> afterTestAsync(
		final ExtensionContext context,
		final Optional<BrowserTCI> optBrowserTCI,
		final Supplier<String> fileSystemFriendlyNameSupplier)
	{
		return optBrowserTCI.map(
				browserTCI ->
				{
					this.logger.debug("Trying to capture video");
					
					return CompletableFuture.runAsync(
						() -> {
							browserTCI.afterTest(
								new TestDescription()
								{
									@Override
									public String getTestId()
									{
										return this.getFilesystemFriendlyName();
									}
									
									@SuppressWarnings("checkstyle:MagicNumber")
									@Override
									public String getFilesystemFriendlyName()
									{
										return fileSystemFriendlyNameSupplier.get();
									}
								}, context.getExecutionException());
							
							this.logger.debug("Finished capture of video");
						},
						TCIExecutorServiceHolder.instance());
				})
			.orElseGet(() -> CompletableFuture.completedFuture(null));
	}
}
