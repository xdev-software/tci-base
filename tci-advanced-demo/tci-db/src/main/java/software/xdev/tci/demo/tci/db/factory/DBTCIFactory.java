package software.xdev.tci.demo.tci.db.factory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.unreliables.Unreliables;

import software.xdev.tci.demo.tci.db.DBTCI;
import software.xdev.tci.demo.tci.db.containers.DBContainer;
import software.xdev.tci.demo.tci.db.containers.DBContainerBuilder;
import software.xdev.tci.factory.prestart.PreStartableTCIFactory;
import software.xdev.tci.factory.prestart.snapshoting.CommitedImageSnapshotManager;
import software.xdev.tci.misc.ContainerMemory;


public class DBTCIFactory extends PreStartableTCIFactory<DBContainer, DBTCI>
{
	public DBTCIFactory()
	{
		this(true);
	}
	
	@SuppressWarnings("resource")
	public DBTCIFactory(final boolean migrateAndInitializeEMC)
	{
		super(
			(c, n) -> new DBTCI(c, n, migrateAndInitializeEMC),
			() -> new DBContainer(DBContainerBuilder.getBuiltImageName())
				.withDatabaseName(DBTCI.DB_DATABASE)
				.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemory(ContainerMemory.M512M)),
			"db-mariadb",
			"container.db",
			"DB");
		this.withSnapshotManager(new CommitedImageSnapshotManager("/var/lib/mysql"));
	}
	
	@Override
	protected void postProcessNew(final DBTCI infra)
	{
		// Docker needs a few milliseconds (usually less than 100) to reconfigure its networks
		// In the meantime existing connections might fail if we go on immediately
		// So let's wait a moment here until everything is fine
		Unreliables.retryUntilSuccess(
			10,
			TimeUnit.SECONDS,
			() -> {
				final String testQuery = infra.getContainer().getTestQueryString();
				try(final Connection con = infra.createDataSource().getConnection();
					final Statement statement = con.createStatement())
				{
					statement.executeQuery(testQuery).getMetaData();
				}
				
				if(infra.isMigrateAndInitializeEMC())
				{
					// Check EMC if pool connections work
					infra.useNewEntityManager(em -> em.createNativeQuery(testQuery).getResultList());
				}
				return null;
			});
	}
}
