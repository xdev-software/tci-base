package software.xdev.tci.demo.entities;

import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(IdentifiableEntity.class)
public abstract class IdentifiableEntity_ {

	public static final String ID = "id";

	
	/**
	 * @see software.xdev.tci.demo.entities.IdentifiableEntity#id
	 **/
	public static volatile SingularAttribute<IdentifiableEntity, Long> id;
	
	/**
	 * @see software.xdev.tci.demo.entities.IdentifiableEntity
	 **/
	public static volatile MappedSuperclassType<IdentifiableEntity> class_;

}

