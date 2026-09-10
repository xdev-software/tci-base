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
package software.xdev.tci.selenium;

import java.util.function.Supplier;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;


/**
 * This is a convenient default implementation for {@link CapabilityFactory}.
 */
public enum TestBrowser implements CapabilityFactory
{
	FIREFOX(() -> {
		final FirefoxOptions options = new FirefoxOptions();
		
		final FirefoxProfile profile = new FirefoxProfile();
		// Allows to type into console without an annoying SELF XSS popup
		profile.setPreference("devtools.selfxss.count", "100");
		// Ignore panel popup on downloads that block top right UI
		profile.setPreference("browser.download.alwaysOpenPanel", false);
		options.setProfile(profile);
		
		return options;
	}),
	// @formatter:off - Link will get cut off
	/**
	 * NOTE: Chrome does not support downloads from insecure (non-HTTPS) locations.
	 * <p>
	 * Support for this was removed in <a
	 * href="https://github.com/chromium/chromium/commit/9606bbe9acda26fdae5b4c407b47a9d98e593464">9606bbe</a> and no
	 * replacement added ever again.
	 * <p>
	 * To make the confusion perfect: Some files can always be downloaded - disregarding if secure or not - because <a
	 * href="https://github.com/chromium/chromium/blob/962086dbf5a0e90475108a7293443deaef6cc7a3/chrome/browser/download/insecure_download_blocking.cc#L85-L87">
	 * there is a hardcoded list of allowed file extensions</a>.
	 * <p>
	 * As a workaround it's possible to specify <code>--unsafely-treat-insecure-origin-as-secure=http://webapp</code>
	 * HOWEVER this will declare the entire site as "secure" which can have unintended side effects
	 * <p>
	 * It's recommended to NOT use Chrome to execute downloads.
	 * <p>
	 * See also: <a href="https://stackoverflow.com/q/78057740">StackOverflow#78057740</a>
	 */
	// @formatter:on
	CHROME(ChromeOptions::new);
	
	private final Supplier<MutableCapabilities> capabilityFactory;
	
	TestBrowser(final Supplier<MutableCapabilities> driverFactory)
	{
		this.capabilityFactory = driverFactory;
	}
	
	@Override
	public MutableCapabilities createCapabilities()
	{
		return this.capabilityFactory.get();
	}
}
