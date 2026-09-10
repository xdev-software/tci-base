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
package software.xdev.tci.logging;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;


public class JULtoSLF4JRedirector
{
	static final JULtoSLF4JRedirector INSTANCE = new JULtoSLF4JRedirector();
	
	protected boolean installed;
	
	protected JULtoSLF4JRedirector()
	{
	}
	
	protected void redirectInternal()
	{
		if(this.installed)
		{
			return;
		}
		
		this.redirectInternalSync();
	}
	
	protected synchronized void redirectInternalSync()
	{
		if(this.installed)
		{
			return;
		}
		
		if(SLF4JBridgeHandler.isInstalled())
		{
			this.installed = true;
			return;
		}
		
		LoggerFactory.getLogger(JULtoSLF4JRedirector.class).debug("Installing SLF4JBridgeHandler");
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		this.installed = true;
	}
	
	public static void redirect()
	{
		INSTANCE.redirectInternal();
	}
}
