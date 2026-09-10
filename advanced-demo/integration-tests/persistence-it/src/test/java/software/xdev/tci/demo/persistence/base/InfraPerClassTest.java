package software.xdev.tci.demo.persistence.base;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class InfraPerClassTest extends BaseTest
{
	@BeforeAll
	public void beforeAll()
	{
		this.startInfra(this.withDefaultData(), this.additionalMigrationLocations());
	}
	
	protected boolean withDefaultData()
	{
		return true;
	}
	
	protected Collection<String> additionalMigrationLocations()
	{
		return List.of();
	}
	
	@AfterAll
	public void afterAll()
	{
		this.stopInfra();
	}
}
