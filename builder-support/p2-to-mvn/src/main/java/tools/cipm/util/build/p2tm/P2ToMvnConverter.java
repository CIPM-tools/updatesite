package tools.cipm.util.build.p2tm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.openntf.maven.p2.model.P2Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.cipm.util.build.p2tm.mvnosgimap.Coordinate;
import tools.cipm.util.build.p2tm.mvnosgimap.MvnRepositoryIndex;
import tools.cipm.util.build.p2tm.updatesiteparse.UpdateSite;
import tools.cipm.util.build.p2tm.updatesiteparse.UpdateSiteBuilder;

public class P2ToMvnConverter {
	private static final Logger logger = LoggerFactory.getLogger(P2ToMvnConverter.class);

	private static MvnRepositoryIndex mvnRepo;

	public static void main(String[] args) {
		// initMvnRepo();
		// installJarsFromLocalDirectory();
		// installJarsFromRemoteRepository();
		// installJarsFromRepository("", FileConstants.URI_FILE_PREFIX + "", true);
	}

	private static void installJarsFromLocalDirectory() {
		var consideredPath = Paths.get(FileConstants.TARGET_DIR_NAME, FileConstants.TARGET_JARS_DIR_NAME);
		if (Files.notExists(consideredPath)) {
			System.out.println("Cannot consider the directory. It does not exist.");
			return;
		}

		try (var scanner = new Scanner(System.in)) {
			var lastGroupIdContainer = new StringBuilder();
			Files.walk(consideredPath).filter(Files::isRegularFile).forEach(path -> {
				var fileName = path.getFileName().toString();
				if (!fileName.endsWith(FileConstants.JAR_FILE_EXTENSION)) {
					return;
				}

				var fileNameParts = fileName.split("_");

				System.out.println("You need to specify a group ID for the artifact " + fileName
						+ ". Please enter it. Leave the ID empty if the last group ID "
						+ lastGroupIdContainer.toString() + " should be reused.");
				var potentialGroupId = scanner.next();
				if (!potentialGroupId.isBlank()) {
					lastGroupIdContainer.delete(0, lastGroupIdContainer.length());
					lastGroupIdContainer.append(potentialGroupId);
				}

				try {
					installJarLocally(path.toString(), lastGroupIdContainer.toString(), fileNameParts[0],
							fileNameParts[1].substring(0,
									fileNameParts[1].length() - FileConstants.JAR_FILE_EXTENSION.length()));
				} catch (IOException | InterruptedException e) {
					System.out.println("Could not process " + fileName);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void installJarsFromRemoteRepository() {
		String id = System.getProperty("p2tm.id");
		String repoUri = System.getProperty("p2tm.uri");
		if (id == null || repoUri == null) {
			System.out.println("No Id or URI given. Stopping.");
			return;
		}

		installJarsFromRepository(id, repoUri, false);
	}

	private static void installJarsFromRepository(String id, String repoUri, boolean isLocal) {
		P2Repository p2Repo = P2Repository.getInstance(URI.create(repoUri), logger);
		var allBundles = p2Repo.getBundles();

		HttpClient client = HttpClient.newHttpClient();
		for (var bundle : allBundles) {
			System.out.println("Downloading " + bundle.getId());

			try {
				if (isLocal) {
					var bundleUri = bundle.getUri("");
					if (!FileConstants.URI_HTTP_SCHEME.equals(bundleUri.getScheme())
							&& !FileConstants.URI_HTTPS_SCHEME.equals(bundleUri.getScheme())
							&& Files.exists(Paths.get(bundleUri))) {
						installJarLocally(bundleUri.getSchemeSpecificPart(), id, bundle.getId(), bundle.getVersion());
					}
					continue;
				}

				var tempStorageFile = Paths.get(FileConstants.TARGET_DIR_NAME,
						bundle.getId() + "_" + bundle.getVersion() + FileConstants.JAR_FILE_EXTENSION);

				if (Files.notExists(tempStorageFile)) {
					var bodyHandler = BodyHandlers.ofFile(tempStorageFile);
					client.send(HttpRequest.newBuilder(bundle.getUri("")).GET().build(), bodyHandler);
				}

				installJarLocally(tempStorageFile.toString(), id, bundle.getId(), bundle.getVersion());

				Files.delete(tempStorageFile);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void installJarLocally(String filePath, String groupId, String artifactId, String version)
			throws IOException, InterruptedException {
		// Only supports Linux for now.

		/*
		 * TODO Use getTransitiveDependencyPOMString(...) in order to generate the POM
		 * file for transitive dependencies. Then include the generated POM in the
		 * process below.
		 */

		var subProcess = new ProcessBuilder("./mvnw", "install:install-file", "-DlocalRepositoryPath=../mvn",
				"-Dfile=" + filePath, "-DgroupId=" + groupId, "-DartifactId=" + artifactId, "-Dversion=" + version,
				"-Dpackaging=jar", "-DcreateChecksum=true").inheritIO().start();
		var subProcessResult = subProcess.waitFor();
		System.out.println(subProcessResult);
	}

	/**
	 * Initialises the local Maven repository's index, i.e. a list of all available
	 * dependencies therein.
	 * 
	 * @see {@link Coordinate}
	 */
	private static void initMvnRepo() {
		// Execution path
		var currentPath = new File("").toPath().toAbsolutePath();

		// The top level directory of the entire GIT repository
		var topDirPath = currentPath.getParent().getParent();

		// Relative path to local Maven repository
		var mvnDirPath = currentPath.relativize(topDirPath.resolve("mvn"));

		// Build the Maven Repository Index
		//
		// The "org.pcm.headless.api" bundle is the only exception in the local Maven
		// repository that only declares the manifest version, hence its mapping is
		// manually added.
		mvnRepo = new MvnRepositoryIndex(mvnDirPath, Map.of("api", "org.pcm.headless.api"));

		// Parse all Maven coordinates available under the local Maven repository
		mvnRepo.build();
	}

	/**
	 * Generates the String content of the POM file, which declares all transitive
	 * dependencies needed for the concrete CIPM update site under
	 * concreteUpdateSitePath. Uses the Maven repository under
	 * relativeLocalMvnRepoDirPath to locate the JAR files associated with the
	 * transitive dependencies.
	 * 
	 * <p>
	 * FIXME The generated POM file content is currently only manually tested by
	 * copy-pasting it into POM files of CIPM and removing the required bundles and
	 * imported packages from the manifest files of CIPM.
	 * 
	 * @param relativeLocalMvnRepoDirPath Relative path to the Maven directory
	 *                                    ("mvn" directory) of the overall update
	 *                                    site (currently this entire repository).
	 *                                    Must be relative to the execution
	 *                                    directory. Currently {@code "../../mvn"}.
	 * @param concreteUpdateSitePath      Path to the concrete CIPM update site
	 *                                    (e.g. {@code archive/cipm-0.1.1}). Will be
	 *                                    converted to absolute path, if not already
	 *                                    an absolute path
	 * @param pomGroupID                  The group ID of the generated POM file for
	 *                                    transitive dependencies
	 * @param pomArtifactID               The artifact ID of the generated POM file
	 *                                    for transitive dependencies
	 * @param pomVersion                  The version of the generated POM file for
	 *                                    transitive dependencies
	 * @return The content of the POM for the transitive dependencies (as String).
	 */
	private static String getTransitiveDependencyPOMString(Path relativeLocalMvnRepoDirPath,
			Path concreteUpdateSitePath, String pomGroupID, String pomArtifactID, String pomVersion) {
		var absConcreteUpdateSitePath = concreteUpdateSitePath.toAbsolutePath();

		// Parse the concrete update site into an in-memory model.
		UpdateSite updateSite;
		try {
			updateSite = new UpdateSiteBuilder().buildLocal(absConcreteUpdateSitePath.toString());
		} catch (IOException e) {
			throw new RuntimeException("Could not parse update site", e);
		}

		// Retrieve all P2 bundles from the parsed update site
		var allBundles = updateSite.bundlesByName.values().stream().collect(Collectors.toList());

		TransitiveDependencyResolver resolver = new TransitiveDependencyResolver(mvnRepo);

		Set<Coordinate> allTransitiveDeps = new HashSet<>();

		// Resolve all transitive dependencies
		for (var p2 : allBundles) {
			List<Coordinate> transitiveDepsForBundle = p2 != null ? resolver.resolve(p2) : List.of();
			allTransitiveDeps.addAll(transitiveDepsForBundle);
		}

		return PomWriter.render(pomGroupID, pomArtifactID, pomVersion, allTransitiveDeps);
	}
}