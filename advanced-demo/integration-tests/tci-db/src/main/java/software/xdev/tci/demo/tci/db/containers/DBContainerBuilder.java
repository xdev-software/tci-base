package software.xdev.tci.demo.tci.db.containers;

import java.nio.file.Paths;
import java.time.Duration;

import software.xdev.tci.imagebuild.BuildImage;


public final class DBContainerBuilder
{
	private DBContainerBuilder()
	{
	}
	
	public static String getImageName()
	{
		return BuildImage.nativeImage(
			"tci-demo-db",
			Duration.ofMinutes(5),
			builder -> builder
				.withDockerFilePath(Paths.get("../tci-db/Dockerfile"))
				.withBaseDir(Paths.get("../"))
				.configureFilesToTransferHandler(h -> h
					.withPostGitIgnoreLines(
						// Ignore everything
						"**")
					.withBaseDirRelativeIgnoreFile(null)
				)
		);
	}
}
