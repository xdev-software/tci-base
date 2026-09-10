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
package software.xdev.tci.selenium.factory.config;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.config.DefaultConfig;
import software.xdev.tci.selenium.factory.BrowserTCIFactory;
import software.xdev.testcontainers.selenium.containers.browser.BrowserWebDriverContainer;


public class DefaultBrowserTCIFactoryConfig extends DefaultConfig implements BrowserTCIFactoryConfig
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultBrowserTCIFactoryConfig.class);
	
	/**
	 * @deprecated Use non legacy option instead
	 */
	@Deprecated(since = "4.0.0")
	public static final String LEGACY_RECORD_MODE = "recordMode";
	/**
	 * @deprecated Use non legacy option instead
	 */
	@Deprecated(since = "4.0.0")
	public static final String LEGACY_RECORD_DIR = "recordDir";
	/**
	 * @deprecated Use non legacy option instead
	 */
	@Deprecated(since = "4.0.0")
	public static final String LEGACY_VNC_ENABLED = "vncEnabled";
	/**
	 * @deprecated Use non legacy option instead
	 */
	@Deprecated(since = "4.0.0")
	public static final String LEGACY_BIDI_ENABLED = "bidiEnabled";
	/**
	 * @deprecated Use non legacy option instead
	 */
	@Deprecated(since = "4.0.0")
	public static final String LEGACY_DEACTIVATE_CDP_IF_POSSIBLE = "deactivateCdpIfPossible";
	/**
	 * @deprecated Use non legacy option instead
	 */
	@Deprecated(since = "4.0.0")
	public static final String LEGACY_BROWSER_CONSOLE_LOG_LEVEL = "browserConsoleLogLevel";
	
	public static final String RECORD_MODE = "record-mode";
	public static final String RECORD_DIR = "record-dir";
	public static final String VNC_ENABLED = "vnc-enabled";
	public static final String DEACTIVATE_CDP_IF_POSSIBLE = "deactivate-cdp-if-possible";
	public static final String MIN_BROWSER_CONSOLE_LOG_LEVEL = "min-browser-console-log-level";
	
	public static final String DEFAULT_RECORD_DIR = "target/records";
	
	protected BrowserWebDriverContainer.RecordingMode systemRecordingMode;
	protected Path dirForRecords;
	protected Boolean vncEnabled;
	protected Boolean bidiEnabled;
	protected Boolean deactivateCdpIfPossible;
	protected BrowserTCIFactory.BrowserConsoleLogLevel browserConsoleLogLevel;
	
	@Override
	protected String propertyNamePrefix()
	{
		return "tci.selenium";
	}
	
	@Override
	public BrowserWebDriverContainer.RecordingMode recordingMode()
	{
		if(this.systemRecordingMode == null)
		{
			final String resolvedRecordMode = this.resolve(RECORD_MODE)
				.or(() -> this.resolve(LEGACY_RECORD_MODE)
					.map(v -> this.reportLegacyConfigOption(LEGACY_RECORD_MODE, RECORD_MODE, v)))
				.orElse(null);
			this.systemRecordingMode = Stream.of(BrowserWebDriverContainer.RecordingMode.values())
				.filter(rm -> rm.toString().equals(resolvedRecordMode))
				.findFirst()
				.orElse(BrowserWebDriverContainer.RecordingMode.RECORD_FAILING);
			LOG.info("Default Recording Mode='{}'", this.systemRecordingMode);
		}
		return this.systemRecordingMode;
	}
	
	@Override
	public Path dirForRecords()
	{
		if(this.dirForRecords == null)
		{
			this.dirForRecords = Path.of(this.resolve(RECORD_DIR)
				.or(() -> this.resolve(LEGACY_RECORD_DIR)
					.map(v -> this.reportLegacyConfigOption(LEGACY_RECORD_DIR, RECORD_DIR, v)))
				.orElse(DEFAULT_RECORD_DIR));
			final boolean wasCreated = this.dirForRecords.toFile().mkdirs();
			LOG.info(
				"Default directory for records='{}', created={}", this.dirForRecords.toAbsolutePath(),
				wasCreated);
		}
		
		return this.dirForRecords;
	}
	
	@Override
	public boolean vncEnabled()
	{
		if(this.vncEnabled == null)
		{
			this.vncEnabled = this.resolveBool(VNC_ENABLED)
				.or(() -> this.resolveBool(LEGACY_VNC_ENABLED)
					.map(v -> this.reportLegacyConfigOption(LEGACY_VNC_ENABLED, VNC_ENABLED, v)))
				.orElse(false);
			LOG.info("VNC enabled={}", this.vncEnabled);
		}
		return this.vncEnabled;
	}
	
	@Override
	public boolean deactivateCdpIfPossible()
	{
		if(this.deactivateCdpIfPossible == null)
		{
			this.deactivateCdpIfPossible = this.resolveBool(DEACTIVATE_CDP_IF_POSSIBLE, true);
			LOG.info("DeactivateCDPIfPossible={}", this.deactivateCdpIfPossible);
		}
		return this.deactivateCdpIfPossible;
	}
	
	@Override
	public BrowserTCIFactory.BrowserConsoleLogLevel minBrowserConsoleLogLevel()
	{
		if(this.browserConsoleLogLevel == null)
		{
			this.browserConsoleLogLevel = this.resolve(MIN_BROWSER_CONSOLE_LOG_LEVEL)
				.or(() -> this.resolve(LEGACY_BROWSER_CONSOLE_LOG_LEVEL)
					.map(v -> this.reportLegacyConfigOption(
						LEGACY_BROWSER_CONSOLE_LOG_LEVEL,
						MIN_BROWSER_CONSOLE_LOG_LEVEL,
						v))
				)
				.map(BrowserTCIFactory.BrowserConsoleLogLevel::valueOf)
				.orElse(BrowserTCIFactory.BrowserConsoleLogLevel.ERROR);
			LOG.info("BrowserConsoleLogLevel={}", this.browserConsoleLogLevel);
		}
		return this.browserConsoleLogLevel;
	}
}
