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
package software.xdev.tci.startup.error.java.fatal;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;

import software.xdev.tci.concurrent.Suppliers;


/**
 * Utility to log contents of java hs_err_pid files of a container that failed to start.
 * <p>
 * Additionally also copies the file into <code>target/has_err_pid/&lt;containerId&gt;/&lt;hs_err_pid-file&gt;</code>
 * </p>
 */
public class HsErrPidStartUpCrashReporter
{
	protected static final String HS_ERR_PID = "hs_err_pid";
	protected static final Supplier<Pattern> DEFAULT_HS_ERR_PID_EXTRACTION_PATTERN_SUPPLIER =
		Suppliers.memoize(() -> Pattern.compile("^# (\\S{1,999}hs_err_pid\\d{1,9}\\.log)$", Pattern.MULTILINE));
	
	protected final GenericContainer<?> container;
	protected final String hsErrPidSearchKey;
	protected final Supplier<Pattern> hsErrPidExtractionPattern;
	protected Supplier<Path> hsErrPidPathSupplier = Suppliers.memoize(
		() -> Paths.get("target/has_err_pid"));
	
	protected boolean startedWithoutCrashing;
	
	public HsErrPidStartUpCrashReporter(final GenericContainer<?> container)
	{
		this(container, HS_ERR_PID, DEFAULT_HS_ERR_PID_EXTRACTION_PATTERN_SUPPLIER);
	}
	
	protected HsErrPidStartUpCrashReporter(
		final GenericContainer<?> container,
		final String hsErrPidSearchKey,
		final Supplier<Pattern> hsErrPidExtractionPattern)
	{
		this.container = container;
		this.hsErrPidSearchKey = hsErrPidSearchKey;
		this.hsErrPidExtractionPattern = hsErrPidExtractionPattern;
	}
	
	public void containerIsStarted()
	{
		this.startedWithoutCrashing = true;
	}
	
	public void containerIsStopping(final Logger logger)
	{
		if(this.startedWithoutCrashing || !logger.isWarnEnabled())
		{
			return;
		}
		
		final String logs = this.container.getLogs();
		if(!logs.contains(this.hsErrPidSearchKey))
		{
			return;
		}
		
		final Matcher matcher = this.hsErrPidExtractionPattern.get().matcher(logs);
		if(!matcher.find())
		{
			logger.warn(
				"Detected {} in log output but was unable to determine the file location - "
					+ "No regex match", this.hsErrPidSearchKey);
			return;
		}
		
		final String filePath = matcher.group(1);
		if(filePath == null || filePath.isBlank())
		{
			logger.warn(
				"Detected {} in log output but was unable to determine the file location - "
					+ "No regex group match", this.hsErrPidSearchKey);
			return;
		}
		
		try
		{
			final String containerId = this.container.getContainerId();
			
			final Path hsErrPidContainerDirPath = this.hsErrPidPathSupplier.get().resolve(containerId);
			Files.createDirectories(hsErrPidContainerDirPath);
			final Path targetHsErrPidFilePath =
				hsErrPidContainerDirPath.resolve(Path.of(filePath).getFileName().toString());
			
			this.container.copyFileFromContainer(
				filePath,
				is -> Files.copy(
					is,
					targetHsErrPidFilePath,
					StandardCopyOption.REPLACE_EXISTING));
			
			logger.warn(
				"Detected {} file[container={}]: {} -> {} \n{}",
				this.hsErrPidSearchKey,
				containerId,
				filePath,
				targetHsErrPidFilePath,
				this.container.copyFileFromContainer(
					filePath,
					is -> IOUtils.toString(is, StandardCharsets.UTF_8)));
		}
		catch(final Exception ex)
		{
			logger.warn("Failed to read {} file {}", this.hsErrPidSearchKey, filePath, ex);
		}
	}
}
