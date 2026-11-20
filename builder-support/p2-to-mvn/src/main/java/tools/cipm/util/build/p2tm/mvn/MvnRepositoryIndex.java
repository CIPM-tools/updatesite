package tools.cipm.util.build.p2tm.mvn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import tools.cipm.util.build.p2tm.FileConstants;
import tools.cipm.util.build.p2tm.p2.ManifestReader;

/**
 * Builds a flat, complete index of a local Maven repository as a single list of
 * {@link MvnEntry}s.
 * 
 * <p>
 * A call to {@link #build()} is necessary to build the Maven repository index.
 */
public class MvnRepositoryIndex {
	private final Path relativeMavenRepoRootPath;
	private Set<MvnEntry> coordList;

	public MvnRepositoryIndex(Path relativeMavenRepoRootPath) {
		this.relativeMavenRepoRootPath = relativeMavenRepoRootPath;
	}

	public Collection<MvnEntry> getMvnEntries() {
		return coordList;
	}

	/**
	 * Builds one {@link MvnEntry} per installed jar in the repository.
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
	 * @param relativeMavenRepoRootPath The repository root relative to
	 *                                  the execution path
	 * @return the list of entries
	 * @throws IOException if the Maven repository cannot be walked
	 */
	public Collection<MvnEntry> build() {
		if (coordList != null) {
			return coordList;
		}

		coordList = new HashSet<>();

		try (var stream = Files.walk(relativeMavenRepoRootPath)) {
			stream
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(FileConstants.JAR_FILE_EXTENSION))
				.map(this::extractEntry)
				.filter(entry -> entry != null)
				.forEach(coordList::add);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}

		return coordList;
	}

	/**
	 * Derives groupId, artifactId, version and bundle name from a single jar's
	 * repository path and manifest.
	 * 
	 * @param jarPath                   The path to the JAR file
	 * @param relativeMavenRepoRootPath The repository root relative to
	 *                                  the execution path
	 * @return An entry or null for the given JAR file (at jarPath).
	 */
	private MvnEntry extractEntry(Path jarPath) {
		// Derive the shared Maven identity
		Path artifactDir = jarPath.getParent();
		if (artifactDir == null) {
			return null;
		}

		
		Path artifactIdDir = artifactDir.getParent();
		Path groupPath = artifactIdDir == null ? null : artifactIdDir.getParent();
		var mavenRepoRootDepth = relativeMavenRepoRootPath.getNameCount();
		if (artifactIdDir == null || groupPath == null || groupPath.getNameCount() < mavenRepoRootDepth) {
			return null;
		}

		String artifactId = artifactIdDir.getFileName().toString();
		String groupId = relativeMavenRepoRootPath.relativize(groupPath).toString().replace(File.separatorChar, '.');
		String version = artifactDir.getFileName().toString();

		String fileName = jarPath.getFileName().toString();
		if (fileName.startsWith(artifactId + "-") && fileName.endsWith(FileConstants.JAR_FILE_EXTENSION)) {
			version = fileName.substring(artifactId.length() + 1,
					fileName.length() - FileConstants.JAR_FILE_EXTENSION.length());
		}

		// --- bundle name (manifest or artifactId fallback) ---
		var manifestHeaders = ManifestReader.readManifest(jarPath);
		var bsnd = ManifestReader.readBundleSymbolicName(manifestHeaders);
		String bundleName = bsnd.orElse(artifactId);

		return new MvnEntry(groupId, artifactId, version, bundleName, ManifestReader.readExportedPackageNames(manifestHeaders));
	}

	/**
	 * @return A list of all Maven entries within the Maven repository with the
	 *         matching bundle name. Returns null, if the Maven repository is not
	 *         built or the bundle cannot be found.
	 */
	public MvnEntry findByBundleName(String bundleName) {
		if (coordList == null) {
			return null;
		}

		return coordList
			.stream()
			.filter(entry -> entry.getBundleName().equals(bundleName))
			.findFirst()
			.orElse(null);
	}
}
