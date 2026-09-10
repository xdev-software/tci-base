package software.xdev.tci.demo.persistence.base;

import org.junit.jupiter.api.AfterEach;


public abstract class InfraPerCaseTest extends BaseTest
{
	@AfterEach
	void afterEach()
	{
		this.stopInfra();
	}
}
