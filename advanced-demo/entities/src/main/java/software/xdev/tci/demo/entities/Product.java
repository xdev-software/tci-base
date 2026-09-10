package software.xdev.tci.demo.entities;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "product")
public class Product extends IdentifiableEntity
{
	@Column(name = "name", nullable = false, unique = true)
	private String name;
	
	public Product()
	{
	}
	
	public Product(final String name)
	{
		this.setName(name);
	}
	
	public String getName()
	{
		return this.name;
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public void setName(final String name)
	{
		Objects.requireNonNull(name);
		if(name.length() > 255)
		{
			throw new IllegalArgumentException("name too long");
		}
		this.name = name;
	}
}
