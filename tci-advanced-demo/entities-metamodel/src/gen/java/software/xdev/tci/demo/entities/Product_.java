package software.xdev.tci.demo.entities;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Product.class)
public abstract class Product_ extends software.xdev.tci.demo.entities.IdentifiableEntity_ {

	
	/**
	 * @see software.xdev.tci.demo.entities.Product#name
	 **/
	public static volatile SingularAttribute<Product, String> name;
	
	/**
	 * @see software.xdev.tci.demo.entities.Product
	 **/
	public static volatile EntityType<Product> class_;

	public static final String NAME = "name";

}

