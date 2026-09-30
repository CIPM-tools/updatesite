package tools.cipm.util.build.p2tm.mvnosgimap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import tools.cipm.util.build.p2tm.FileConstants;
import tools.cipm.util.build.p2tm.ManifestReader;

/**
 * Builds a flat, complete index of a local Maven repository as a single list of
 * {@link Coordinate}s. Parses the index based on the JAR files in the local
 * Maven repository instead of the provided items.json file, because it does not
 * contain any information on the exported packages the individual Maven
 * dependencies.
 * 
 * <p>
 * A call to {@link #build()} is necessary to build the Maven repository index.
 */
public class MvnRepositoryIndex {
	/**
	 * Only for artifacts whose "bundle name" is a repository convention and not
	 * discoverable from the artifact itself (e.g. plain non-OSGi jars that have no
	 * Bundle-SymbolicName in the manifest). Keyed by Maven artifactId.
	 */
	private final Map<String, String> artifactIdToBundleNameOverrides;

	private final Path relativeMavenRepoRootPath;
	private Set<Coordinate> coordList;

	public MvnRepositoryIndex(Path relativeMavenRepoRootPath, Map<String, String> artifactIdToBundleNameOverrides) {
		this.relativeMavenRepoRootPath = relativeMavenRepoRootPath;
		this.artifactIdToBundleNameOverrides = artifactIdToBundleNameOverrides == null ? Map.of()
				: artifactIdToBundleNameOverrides;
	}

	public MvnRepositoryIndex(Path relativeMavenRepoRootPath) {
		this(relativeMavenRepoRootPath, null);
	}

	public Collection<Coordinate> getMvnRepositoryIndices() {
		return coordList;
	}

	/**
	 * Builds one {@link Coordinate} per installed bundle in the repository (in the
	 * form of a JAR file).
	 *
	 * <p>
	 * Each Maven artifact is stored at:
	 * 
	 * <pre>
	 *   &lt;relativeMavenRepoRootPath&gt;/&lt;groupPath&gt;/&lt;artifactId&gt;/&lt;version&gt;/&lt;artifactId&gt;-&lt;version&gt;.jar
	 * </pre>
	 *
	 * A single walk of the Maven repository derives every field from that layout
	 * plus the jar's OSGi {@code Bundle-SymbolicName}.
	 *
	 * @param relativeMavenRepoRootPath The repository root (currently
	 *                                  {@code Paths.get("../../mvn")}) relative to
	 *                                  the execution path
	 * @return the list of coordinates for every installed bundle
	 * @throws IOException if the Maven repository cannot be walked
	 */
	public Collection<Coordinate> build() {
		if (coordList != null) {
			return coordList;
		}

		var coordinates = new HashSet<Coordinate>();

		try (var stream = Files.walk(relativeMavenRepoRootPath)) {
			stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(FileConstants.JAR_FILE_EXTENSION))
					.forEach(jarPath -> coordinates.addAll(coordinatesFor(jarPath)));
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}

		coordList = coordinates;
		return coordinates;
	}

	/**
	 * Resets the built Maven repository index. A call to {@link #build()} is
	 * necessary to rebuild it.
	 */
	public void reset() {
		this.coordList = null;
	}

	/**
	 * Derives groupId, artifactId, version and bundle name from a single jar's
	 * repository path and manifest.
	 * 
	 * @param jarPath                   The path to the JAR file
	 * @param relativeMavenRepoRootPath The repository root (currently
	 *                                  {@code Paths.get("../../mvn")}) relative to
	 *                                  the execution path
	 * @return A list of all Maven coordinates for the given JAR file (at jarPath).
	 *         The entire JAR file (i.e. the entire bundle) will have one coordinate
	 *         and each of its exported packages will have their own coordinates.
	 */
	private List<Coordinate> coordinatesFor(Path jarPath) {
		// Derive the shared Maven identity
		Path artifactDir = jarPath.getParent();
		if (artifactDir == null)
			return List.of();

		String versionDir = artifactDir.getFileName().toString();
		Path artifactIdDir = artifactDir.getParent();
		Path groupPath = artifactIdDir == null ? null : artifactIdDir.getParent();
		var mavenRepoRootDepth = relativeMavenRepoRootPath.getNameCount();
		if (artifactIdDir == null || groupPath == null || groupPath.getNameCount() < mavenRepoRootDepth) {
			return List.of();
		}

		String artifactId = artifactIdDir.getFileName().toString();
		String groupId = relativeMavenRepoRootPath.relativize(groupPath).toString().replace(File.separatorChar, '.');

		String fileName = jarPath.getFileName().toString();
		String version = versionDir;
		if (fileName.startsWith(artifactId + "-") && fileName.endsWith(FileConstants.JAR_FILE_EXTENSION)) {
			version = fileName.substring(artifactId.length() + 1,
					fileName.length() - FileConstants.JAR_FILE_EXTENSION.length());
		}

		// --- bundle name (manifest, or curated override, or artifactId fallback) ---
		var manifestHeaders = ManifestReader.readManifest(jarPath);
		var bsnd = ManifestReader.readBundleSymbolicName(manifestHeaders);
		String bundleName = bsnd.isPresent() ? bsnd.get().symbolicName
				: artifactIdToBundleNameOverrides.getOrDefault(artifactId, artifactId);

		List<Coordinate> result = new ArrayList<>();

		// One Coordinate for the bundle itself
		result.add(new Coordinate(groupId, artifactId, version, bundleName));

		// One Coordinate per exported package
		for (String pkg : ManifestReader.readExportedPackageNames(manifestHeaders)) {
			result.add(new Coordinate(groupId, artifactId, version, pkg, bundleName));
		}
		return result;
	}

	/**
	 * @return A list of all Maven coordinates within the Maven repository. Returns
	 *         null, if the Maven repository is not built.
	 */
	public List<Coordinate> findAllBundles() {
		return coordList != null ? coordList.stream().filter(c -> !c.hasPackage()).collect(Collectors.toList()) : null;
	}

	/**
	 * @return A list of all Maven coordinates within the Maven repository with the
	 *         matching bundle name. Returns null, if the Maven repository is not
	 *         built.
	 */
	public List<Coordinate> findByBundleName(String bundleName) {
		return coordList != null
				? coordList.stream().filter(c -> c.bundleName.equals(bundleName)).collect(Collectors.toList())
				: null;
	}
}