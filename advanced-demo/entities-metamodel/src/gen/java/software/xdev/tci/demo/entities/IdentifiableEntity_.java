package software.xdev.tci.demo.entities;

import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link software.xdev.tci.demo.entities.IdentifiableEntity}
 **/
@StaticMetamodel(IdentifiableEntity.class)
public abstract class IdentifiableEntity_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";

	
	/**
	 * Static metamodel type for {@link software.xdev.tci.demo.entities.IdentifiableEntity}
	 **/
	public static volatile MappedSuperclassType<IdentifiableEntity> class_;
	
	/**
	 * Static metamodel for attribute {@link software.xdev.tci.demo.entities.IdentifiableEntity#id}
	 **/
	public static volatile SingularAttribute<IdentifiableEntity, Long> id;

}

