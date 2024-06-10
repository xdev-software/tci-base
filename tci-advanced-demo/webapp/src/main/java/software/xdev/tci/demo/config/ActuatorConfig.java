package software.xdev.tci.demo.config;

import jakarta.validation.constraints.NotBlank;


public class ActuatorConfig
{
	@NotBlank
	private String username;
	
	@NotBlank
	private String passwordHash;
	
	public String getUsername()
	{
		return this.username;
	}
	
	public void setUsername(final String username)
	{
		this.username = username;
	}
	
	public String getPasswordHash()
	{
		return this.passwordHash;
	}
	
	public void setPasswordHash(final String passwordHash)
	{
		this.passwordHash = passwordHash;
	}
	
	@Override
	public String toString()
	{
		return "ActuatorConfig [username=" + this.username + ", passwordHash=***]";
	}
}
