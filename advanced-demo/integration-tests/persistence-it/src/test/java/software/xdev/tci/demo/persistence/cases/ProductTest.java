package software.xdev.tci.demo.persistence.cases;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import software.xdev.tci.demo.entities.Product;
import software.xdev.tci.demo.persistence.base.InfraPerCaseTest;
import software.xdev.tci.demo.persistence.jpa.dao.ProductDAO;


class ProductTest extends InfraPerCaseTest
{
	@Autowired
	ProductDAO productDAO;
	
	@Test
	void newProductWorkflow()
	{
		this.startInfra();
		
		final String name = "TEST2";
		final Product product = Assertions.assertDoesNotThrow(() -> this.productDAO.save(new Product(name)));
		
		final long id = product.getId();
		Assertions.assertTrue(id > 0);
		Assertions.assertNotNull(this.productDAO.findByName(name));
		
		Assertions.assertEquals(1, this.productDAO.deleteById(id));
		
		Assertions.assertNull(this.productDAO.findByName(name));
		
		Assertions.assertEquals(0, this.productDAO.deleteById(id));
	}
}
