package software.xdev.tci.demo.webapp.datageneration;

import jakarta.persistence.EntityManager;

import software.xdev.tci.demo.entities.Product;
import software.xdev.tci.demo.persistence.jpa.dao.ProductDAO;
import software.xdev.tci.demo.tci.db.datageneration.AbstractDBDataGenerator;


public class ProductDG extends AbstractDBDataGenerator
{
	public ProductDG(final EntityManager em)
	{
		super(em);
	}
	
	public Product generateProduct(final String name)
	{
		return this.save(ProductDAO::new, new Product(name));
	}
}
