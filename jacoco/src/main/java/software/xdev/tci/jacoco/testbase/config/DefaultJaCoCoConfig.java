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
package software.xdev.tci.jacoco.testbase.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.tci.config.DefaultConfig;


public class DefaultJaCoCoConfig extends DefaultConfig implements JaCoCoConfig
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultJaCoCoConfig.class);
	
	public static final String EXECUTION_DATA_FILES_DIR = "execution-data-files-dir";
	public static final String MOVE_OLD_EXECUTION_DATA_FILES_DIR = "move-old" + EXECUTION_DATA_FILES_DIR;
	public static final String EXECUTION_DATA_FILES_DIR_OLD = EXECUTION_DATA_FILES_DIR + "-old";
	public static final String EXECUTION_DATA_FILE_SUFFIX = "execution-data-file-suffix";
	
	public static final String DEFAULT_EXECUTION_DATA_FILES_DIR = "target/jacoco-execution-data-files";
	public static final String DEFAULT_EXECUTION_DATA_FILES_DIR_OLD = DEFAULT_EXECUTION_DATA_FILES_DIR + "-old";
	public static final String DEFAULT_EXECUTION_DATA_FILE_SUFFIX = "-jacoco.exec";
	
	protected Boolean enabled;
	protected Path dirForExecutionDataFiles;
	protected String executionDataFileSuffix;
	
	@Override
	protected String propertyNamePrefix()
	{
		return "tci.jacoco";
	}
	
	@Override
	public boolean enabled()
	{
		if(this.enabled == null)
		{
			this.enabled = this.resolveBool("enabled", false);
		}
		return this.enabled;
	}
	
	@Override
	public Path dirForExecutionDataFiles()
	{
		if(this.dirForExecutionDataFiles == null)
		{
			this.initDirForExecutionDataFiles();
		}
		
		return this.dirForExecutionDataFiles;
	}
	
	protected void initDirForExecutionDataFiles()
	{
		if(this.dirForExecutionDataFiles != null)
		{
			return;
		}
		
		this.dirForExecutionDataFiles = Path.of(
			this.resolve(EXECUTION_DATA_FILES_DIR).orElse(DEFAULT_EXECUTION_DATA_FILES_DIR));
		
		if(this.resolveBool(MOVE_OLD_EXECUTION_DATA_FILES_DIR, true)
			&& Files.exists(this.dirForExecutionDataFiles))
		{
			final Path oldDir =
				Path.of(this.resolve(EXECUTION_DATA_FILES_DIR_OLD).orElse(DEFAULT_EXECUTION_DATA_FILES_DIR_OLD));
			try
			{
				if(Files.exists(oldDir))
				{
					FileUtils.deleteDirectory(oldDir.toFile());
				}
				Files.move(this.dirForExecutionDataFiles, oldDir);
			}
			catch(final Exception ex)
			{
				LOG.warn("Failed to move {} to {}", this.dirForExecutionDataFiles, oldDir, ex);
			}
		}
		
		LOG.info(
			"Default directory that will be used for execution-data-files='{}'",
			this.dirForExecutionDataFiles.toAbsolutePath());
	}
	
	@Override
	public String executionDataFileSuffix()
	{
		if(this.executionDataFileSuffix == null)
		{
			this.executionDataFileSuffix =
				this.resolve(EXECUTION_DATA_FILE_SUFFIX).orElse(DEFAULT_EXECUTION_DATA_FILE_SUFFIX);
		}
		return this.executionDataFileSuffix;
	}
}
