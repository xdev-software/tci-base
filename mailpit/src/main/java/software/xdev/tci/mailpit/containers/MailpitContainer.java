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
package software.xdev.tci.mailpit.containers;

import java.util.Map;
import java.util.stream.Collectors;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


public class MailpitContainer extends GenericContainer<MailpitContainer>
{
	public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("axllent/mailpit:v1.30");
	
	public static final int WEB_PORT = 8025;
	public static final int SMTP_PORT = 1025;
	
	public MailpitContainer()
	{
		super(DEFAULT_IMAGE);
		
		// Version check is not needed in tests
		this.addEnv("MP_DISABLE_VERSION_CHECK", "true");
		// Resolving client reverse dns is not needed
		this.addEnv("MP_SMTP_DISABLE_RDNS", "true");
		
		// Enforce CSP (only important when debugging)
		this.addEnv("MP_BLOCK_REMOTE_CSS_AND_FONTS", "true");
		
		// Use normal SMTP
		this.addEnv("MP_SMTP_AUTH_ALLOW_INSECURE", "true");
		
		this.addExposedPort(WEB_PORT);
	}
	
	public MailpitContainer withSmtpAuth(final Map<String, String> usernamePasswords)
	{
		this.addEnv(
			"MP_SMTP_AUTH",
			usernamePasswords.entrySet()
				.stream()
				.map(e -> e.getKey() + ":" + e.getValue())
				.collect(Collectors.joining(" ")));
		return this;
	}
}
