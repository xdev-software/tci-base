package software.xdev.tci.demo.webapp.cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.rnorth.ducttape.unreliables.Unreliables;

import software.xdev.tci.demo.persistence.jpa.dao.ProductDAO;
import software.xdev.tci.demo.webapp.base.InfraPerCaseTest;
import software.xdev.tci.demo.webapp.datageneration.ProductDG;
import software.xdev.tci.selenium.TestBrowser;


class ProductTest extends InfraPerCaseTest
{
	@DisplayName("Create product")
	@ParameterizedTest
	@EnumSource(TestBrowser.class)
	void create(final TestBrowser browser)
	{
		this.startAll(
			browser,
			dbtci -> dbtci.useNewEntityManager(em -> new ProductDG(em).generateProduct("PLACEHOLDER")));
		
		this.loginAndGotoMainSite();
		
		final WebElement taInput = Unreliables.retryUntilSuccess(
			2,
			() -> {
				this.navigateTo("swagger-ui/index.html#/product-controller/create");
				
				// Sometimes the button is clicked but JS doesn't react (likely because it's not loaded yet)
				this.waitUntil(d -> d.findElement(By.className("try-out__btn"))).click();
				return this.waitUntil(d -> d.findElement(By.className("body-param__text")));
			});
		taInput.sendKeys(Keys.CONTROL + "a");
		taInput.sendKeys(Keys.DELETE);
		taInput.sendKeys("""
			{
			  "name": "TEST-ABC"
			}""");
		
		this.waitUntil(d -> d.findElement(By.className("execute"))).click();
		
		final WebElement liveResponseTable = this.waitUntil(d -> d.findElement(By.className("live-responses-table")));
		// First element contains response body
		final String responseText = liveResponseTable.findElements(By.className("microlight")).get(0)
			// getText sometimes does not work with Chrome (Selenium 4.47)
			.getAttribute("textContent");
		// Id should be 2 as another product was created before
		assertEquals(
			"""
			{
			  "id": 2,
			  "name": "TEST-ABC"
			}""", responseText);
		
		this.dbInfra().useNewEntityManager(em -> {
			final ProductDAO productDAO = new ProductDAO(em);
			assertNotNull(productDAO.findByName("TEST-ABC"));
		});
	}
}
