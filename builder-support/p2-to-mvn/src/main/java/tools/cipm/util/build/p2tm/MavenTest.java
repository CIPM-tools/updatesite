package tools.cipm.util.build.p2tm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openntf.maven.p2.model.P2Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.cipm.util.build.p2tm.mvnosgimap.Coordinate;
import tools.cipm.util.build.p2tm.mvnosgimap.MvnRepositoryIndex;
import tools.cipm.util.build.p2tm.updatesiteparse.UpdateSite;
import tools.cipm.util.build.p2tm.updatesiteparse.UpdateSiteBuilder;
import tools.cipm.util.build.p2tm.updatesiteparse.UpdateSiteBundle;

/**
 * A test class that demonstrates how {@link UpdateSite} and
 * {@link MvnRepositoryIndex} can be utilised via
 * {@link TransitiveDependencyResolver}.
 */
public class MavenTest {
	private static final Logger logger = LoggerFactory.getLogger(MavenTest.class);

	@Test
	public void testWithMapping() {
		// Execution path
		var currentPath = new File("").toPath().toAbsolutePath();

		// The top level directory of the entire GIT repository
		var topDirPath = currentPath.getParent().getParent();

		// Relative path to local Maven repository
		var mvnDirPath = currentPath.relativize(topDirPath.resolve("mvn"));

		// Absolute path to the concrete CIPM update site
		var repoPath = topDirPath.resolve("archive").resolve("cipm-0.1.1").toAbsolutePath();

		// Build the Maven Repository Index
		//
		// The "org.pcm.headless.api" bundle is the only exception in the local Maven
		// repository that only declares the manifest version, hence its mapping is
		// manually added.
		var mvnRepo = new MvnRepositoryIndex(mvnDirPath, Map.of("api", "org.pcm.headless.api"));
		mvnRepo.build();

		// Parse the update site into an in-memory model.
		UpdateSite updateSite;
		try {
			updateSite = new UpdateSiteBuilder().buildLocal(repoPath.toString());
		} catch (IOException e) {
			throw new RuntimeException("Could not parse update site", e);
		}

		P2Repository p2Repo = P2Repository.getInstance(URI.create(updateSite.repositoryUri), logger);
		var allBundles = p2Repo.getBundles();

		TransitiveDependencyResolver resolver = new TransitiveDependencyResolver(mvnRepo);

		Set<Coordinate> allTransitiveDeps = new HashSet<>();

		// Resolve all transitive dependencies for all bundles
		for (var p2 : allBundles) {
			UpdateSiteBundle bundle = updateSite.bundlesByName.get(p2.getId());
			List<Coordinate> transitiveDepsForBundle = bundle != null ? resolver.resolve(bundle) : List.of();
			allTransitiveDeps.addAll(transitiveDepsForBundle);
		}

		// Render the POM file content with placeholder groupID, artifactID and version
		// They have to be replaced by actual values in P2ToMvnConverter
		var pom = PomWriter.render("abc", "def", "v0.0.0", allTransitiveDeps);
		System.out.println(pom);
	}
}
