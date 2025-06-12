package software.xdev.tci.demo.tci.db.containers;

import org.testcontainers.utility.DockerImageName;


public class DBContainer extends MariaDBContainer<DBContainer>
{
	public static final int PORT = MARIADB_PORT;
	
	public DBContainer(final String image)
	{
		super(DockerImageName.parse("mariadb:11"));
		
		// (31.03.2022 AB)NOTE: https://github.com/testcontainers/testcontainers-java/issues/914
		// Do not mount the volume!
		
		// DO NOT resolve client hostnames for more performance
		// https://mariadb.com/docs/server/ref/mdb/system-variables/skip_name_resolve/
		this.setCommand("--skip-name-resolve");
	}
}
