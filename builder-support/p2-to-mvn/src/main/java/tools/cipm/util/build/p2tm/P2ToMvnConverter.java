package tools.cipm.util.build.p2tm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.openntf.maven.p2.model.P2Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import tools.cipm.util.build.p2tm.mvn.MvnEntry;
import tools.cipm.util.build.p2tm.mvn.MvnRepositoryIndex;
import tools.cipm.util.build.p2tm.p2.UpdateSiteBuilder;
import tools.cipm.util.build.p2tm.p2.data.UpdateSite;

public class P2ToMvnConverter {
	private static final Logger logger = LoggerFactory.getLogger(P2ToMvnConverter.class);

	private static MvnRepositoryIndex mvnIndex;

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
				"-Dpackaging=jar", "-DcreateChecksum=true").inheritIO().start(); //-DpomFile=
		var subProcessResult = subProcess.waitFor();
		System.out.println(subProcessResult);
	}

	/**
	 * Initialises the local Maven repository's index, i.e. a list of all available
	 * dependencies therein.
	 * 
	 * @see {@link MvnEntry}
	 */
	private static void initMvnIndex() {
		var mvnDirPath = Paths.get("..", "mvn");

		if (Files.notExists(mvnDirPath)) {
			throw new IllegalStateException("mvn repository not found.");
		}

		mvnIndex = new MvnRepositoryIndex(mvnDirPath);
		mvnIndex.build();
	}

	public static void resolveTransitiveDependencies() {
		initMvnIndex();

		// Parse the concrete update site into an in-memory model.
		UpdateSite updateSite;
		try {
			updateSite = UpdateSiteBuilder.build(Paths.get("..", "mvn").toAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException("Could not parse update site", e);
		}

		TransitiveDependencyResolver resolver = new TransitiveDependencyResolver(mvnIndex, updateSite);
		var result = resolver.resolve();

		Map<String, IntermediateUnresolvedEntries> bundleNameToUnresolvedEntries = new HashMap<>();
		Set<MvnEntry> missing = new HashSet<>();
		Set<MvnEntry> withoutGroupId = new HashSet<>();
		result.forEach((bundle, resolutionResult) -> {
			missing.addAll(resolutionResult.missing());
			resolutionResult
				.resolved()
				.stream()
				.filter(entry -> entry.getGroupId().isBlank())
				.forEach(withoutGroupId::add);
			
			if (!missing.isEmpty() || !withoutGroupId.isEmpty()) {
				bundleNameToUnresolvedEntries.put(bundle.getSymbolicName(), new IntermediateUnresolvedEntries(new HashSet<>(withoutGroupId), new HashSet<>(missing)));
			}
			missing.clear();
			withoutGroupId.clear();
		});

		if (!bundleNameToUnresolvedEntries.isEmpty()) {
			Gson gson = new Gson();
			try (var writer = Files.newBufferedWriter(Paths.get("intermediate.json"))) {
				writer.write(gson.toJson(bundleNameToUnresolvedEntries));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static record IntermediateUnresolvedEntries(Set<MvnEntry> withoutGroupId, Set<MvnEntry> missing) {}
}