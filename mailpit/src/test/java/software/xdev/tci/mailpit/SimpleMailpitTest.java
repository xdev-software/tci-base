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
package software.xdev.tci.mailpit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.recipient.RecipientBuilder;

import software.xdev.mailpit.api.MessageApi;
import software.xdev.mailpit.client.ApiClient;
import software.xdev.mailpit.model.Message;
import software.xdev.tci.concurrent.TCIExecutorServiceHolder;
import software.xdev.tci.factory.registry.TCIFactoryRegistry;
import software.xdev.tci.mailpit.containers.MailpitContainer;
import software.xdev.tci.mailpit.factory.MailpitTCIFactory;
import software.xdev.tci.network.LazyNetwork;
import software.xdev.tci.network.LazyNetworkPool;


class SimpleMailpitTest
{
	protected static final MailpitTCIFactory FACTORY = new MailpitTCIFactory(() -> {
		final MailpitContainer container = MailpitTCIFactory.createDefaultContainer();
		// For testing we send mails form the host so SMTP must be exposed
		container.addExposedPort(MailpitTCIFactory.SMTP_PORT);
		return container;
	});
	protected static final LazyNetworkPool LAZY_NETWORK_POOL = new LazyNetworkPool();
	
	protected LazyNetwork network;
	protected MailpitTCI mailpitInfra;
	
	@BeforeAll
	static void beforeAll()
	{
		LAZY_NETWORK_POOL.managePoolAsync();
		
		TCIFactoryRegistry.instance().warmUp();
		
		// Preload classes
		CompletableFuture.runAsync(ApiClient::new, TCIExecutorServiceHolder.instance());
	}
	
	@BeforeEach
	void beforeEach()
	{
		this.network = LAZY_NETWORK_POOL.getNew();
		this.mailpitInfra = FACTORY.getNew(this.network);
	}
	
	@Test
	void check()
	{
		final String toAddress = "m.mustermann@test.localhost";
		final String subject = "Test";
		final String plainText = "This is a test";
		
		try(final Mailer mailer = MailerBuilder.withSMTPServer(
				this.mailpitInfra.getContainer().getHost(),
				this.mailpitInfra.getContainer().getMappedPort(MailpitTCIFactory.SMTP_PORT),
				MailpitTCIFactory.DEFAULT_USER,
				MailpitTCIFactory.DEFAULT_PW)
			.withTransportStrategy(TransportStrategy.SMTP)
			.withDebugLogging(true)
			.buildMailer())
		{
			mailer.sendMail(EmailBuilder.startingBlank()
				.from(MailpitTCIFactory.DEFAULT_USER)
				.withRecipients(new RecipientBuilder()
					.withType(jakarta.mail.Message.RecipientType.TO)
					.withAddress(toAddress)
					.withName("Max Mustermann")
					.build())
				.withSubject(subject)
				.withPlainText(plainText)
				.buildEmail());
		}
		catch(final Exception ex)
		{
			throw new IllegalStateException("Mailer failed", ex);
		}
		
		final Message message = new MessageApi(this.mailpitInfra.apiClient()).getMessageParams("latest");
		assertNotNull(message);
		assertAll(
			() -> assertEquals(toAddress, message.getTo().getFirst().getAddress()),
			() -> assertEquals(subject, message.getSubject()),
			() -> assertEquals(
				plainText,
				Arrays.stream(message.getText().split("\n"))
					.map(String::trim)
					.findFirst()
					.orElse(null))
		);
	}
	
	@AfterEach
	void afterEach()
	{
		if(this.mailpitInfra != null)
		{
			this.mailpitInfra.stop();
			this.mailpitInfra = null;
		}
		if(this.network != null)
		{
			this.network.close();
			this.network = null;
		}
	}
	
	@AfterAll
	static void afterAll()
	{
		FACTORY.close();
		LAZY_NETWORK_POOL.close();
	}
}
