package software.xdev.tci.demo.persistence.cases;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import software.xdev.tci.demo.persistence.base.InfraPerClassTest;
import software.xdev.tci.demo.persistence.jpa.dao.ProductDAO;


class ProductReadonlyTest extends InfraPerClassTest
{
	@Autowired
	ProductDAO productDAO;
	
	@Test
	void list()
	{
		Assertions.assertEquals(1, this.productDAO.findAll().size());
	}
	
	@Test
	void get()
	{
		Assertions.assertAll(
			() -> Assertions.assertNotNull(this.productDAO.findByName("TEST1")),
			() -> Assertions.assertNotNull(this.productDAO.getById(1L))
		);
	}
}
