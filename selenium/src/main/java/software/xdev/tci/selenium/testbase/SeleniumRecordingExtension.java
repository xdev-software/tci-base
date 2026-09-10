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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.junit.jupiter.FileSystemFriendlyName;
import software.xdev.tci.selenium.BrowserTCI;


/**
 * @deprecated Use {@link SeleniumRecorder} instead.
 */
@Deprecated(forRemoval = true)
public abstract class SeleniumRecordingExtension implements AfterTestExecutionCallback
{
	private static final Logger LOG = LoggerFactory.getLogger(SeleniumRecordingExtension.class);
	
	protected final Function<ExtensionContext, BrowserTCI> tciExtractor;
	
	protected SeleniumRecordingExtension(
		final Function<ExtensionContext, BrowserTCI> tciExtractor)
	{
		this.tciExtractor = Objects.requireNonNull(tciExtractor);
	}
	
	@Override
	public void afterTestExecution(final ExtensionContext context)
	{
		new SeleniumRecorder(LOG).afterTestAsync(
			context,
			Optional.ofNullable(this.tciExtractor.apply(context)),
			new FileSystemFriendlyName(context)).join();
	}
}
