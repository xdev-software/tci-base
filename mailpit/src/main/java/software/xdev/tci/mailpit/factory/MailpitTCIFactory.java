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
package software.xdev.tci.mailpit.factory;

import java.util.Map;
import java.util.function.Supplier;

import software.xdev.tci.factory.prestart.PreStartableTCIFactory;
import software.xdev.tci.mailpit.MailpitTCI;
import software.xdev.tci.mailpit.containers.MailpitContainer;
import software.xdev.tci.misc.ContainerMemory;


public class MailpitTCIFactory extends PreStartableTCIFactory<MailpitContainer, MailpitTCI>
{
	public static final String DEFAULT_USER = "no-reply@test.localhost";
	public static final String DEFAULT_PW = "test";
	public static final int SMTP_PORT = MailpitContainer.SMTP_PORT;
	
	public MailpitTCIFactory()
	{
		this(MailpitTCIFactory::createDefaultContainer);
	}
	
	public MailpitTCIFactory(final Supplier<MailpitContainer> mailpitContainerSupplier)
	{
		super(
			MailpitTCI::new,
			mailpitContainerSupplier,
			"mailpit",
			"container.mailpit",
			"Mailpit"
		);
	}
	
	@SuppressWarnings("resource")
	public static MailpitContainer createDefaultContainer()
	{
		return new MailpitContainer()
			.withSmtpAuth(Map.of(DEFAULT_USER, DEFAULT_PW))
			.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemory(ContainerMemory.M512M));
	}
}
