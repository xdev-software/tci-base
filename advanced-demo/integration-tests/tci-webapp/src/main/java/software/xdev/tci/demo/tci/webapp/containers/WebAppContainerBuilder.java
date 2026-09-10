package software.xdev.tci.demo.tci.webapp.containers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import software.xdev.tci.imagebuild.BuildImage;
import software.xdev.tci.jacoco.testbase.config.JaCoCoConfig;
import software.xdev.testcontainers.imagebuilder.transfer.fcm.FileLinesContentModifier;


public final class WebAppContainerBuilder
{
	private WebAppContainerBuilder()
	{
	}
	
	public static String getImageName()
	{
		return BuildImage.nativeImage(
			"tci-demo-webapp",
			Duration.ofMinutes(5),
			builder -> {
				builder
					// NOTE: AOT can't be used properly when JaCoCo is active
					.withBuildArg("ENABLE_AOT", "1")
					.withDockerFilePath(Paths.get("../../integration-tests/tci-webapp/Dockerfile"))
					.withBaseDir(Paths.get("../../"))
					.configureFilesToTransferHandler(h -> h
						.withPostGitIgnoreLines(
							// Ignore git-folder, as it will be provided in the Dockerfile
							".git/**",
							// Ignore other unused folders and extensions
							"*.iml",
							"*.cmd",
							"*.md",
							"_dev_infra/**",
							"_resource_metrics/**",
							// Ignore other Dockerfiles (our required file will always be transferred)
							"Dockerfile",
							// Ignore not required test-modules that may have changed
							// sources only - otherwise the parent pom doesn't find the resources
							"integration-tests/**",
							"**/src/test/**",
							// Ignore resources that are just used for development
							"webapp/src/main/resources-dev/**",
							// Most files from these folders need to be ignored -> Down there for highest prio
							"node_modules",
							"target")
						// File is in root directory - we can't access it
						.withBaseDirRelativeIgnoreFile(null)
						.withTransferArchiveTARCompressorCustomizer(c -> c
							// Rewrite parent pom to exclude integration tests
							// This way changes in test pom's cause no redownload of dependencies
							.withContentModifier(new FileLinesContentModifier()
							{
								@Override
								public boolean shouldApply(
									final Path sourcePath,
									final String targetPath,
									final TarArchiveEntry tarArchiveEntry)
								{
									return "pom.xml".equals(targetPath);
								}
								
								@Override
								public List<String> modify(
									final List<String> lines,
									final Path sourcePath,
									final String targetPath,
									final TarArchiveEntry tarArchiveEntry)
								{
									return lines.stream()
										// Remove integration tests module
										.filter(s -> !s.contains("<module>integration-tests"))
										.toList();
								}
								
								@Override
								public boolean isIdentical(final List<String> original, final List<String> created)
								{
									return original.size() == created.size();
								}
							})));
				
				if(JaCoCoConfig.instance().enabled())
				{
					builder.withBuildArg("JACOCO_AGENT_ENABLED", "1");
				}
				
				return builder;
			});
	}
}
