package tools.cipm.util.build.p2tm.p2;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openntf.maven.p2.model.P2Bundle;
import org.openntf.maven.p2.model.P2Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.cipm.util.build.p2tm.FileConstants;
import tools.cipm.util.build.p2tm.p2.data.BundleDependency;
import tools.cipm.util.build.p2tm.p2.data.BundlePackage;
import tools.cipm.util.build.p2tm.p2.data.UpdateSite;
import tools.cipm.util.build.p2tm.p2.data.UpdateSiteBundle;

/**
 * Builds a complete {@link UpdateSite} by combining p2-layout-resolver
 * (UpdateSiteBundle enumeration) with org.eclipse.osgi.util.ManifestElement
 * (accurate manifest parsing).
 */
public final class UpdateSiteBuilder {
	private static final Logger logger = LoggerFactory.getLogger(UpdateSiteBuilder.class);

	/**
	 * Builds the in-memory representation of a local updatesite clone.
	 *
	 * @param localCloneRoot the filesystem path to the cloned update site
	 *                       (containing artifacts.xml/jar and plugins/)
	 */
	public static UpdateSite build(String localCloneRoot) throws IOException {
		// P2Repository handles file:// transparently via P2Util (non-HTTP branch).
		var repositoryUri = Paths.get(localCloneRoot).toAbsolutePath().toUri();
		P2Repository repo = P2Repository.getInstance(repositoryUri, logger);

		Map<String, UpdateSiteBundle> bundlesByName = new LinkedHashMap<>();
		Map<BundlePackage, List<UpdateSiteBundle>> packagesToBundles = new LinkedHashMap<>();

		for (P2Bundle p2 : repo.getBundles()) {
			// P2Bundle.getUri("") for a file:// base yields
			// file:///<root>/plugins/<id>_<version>.jar
			// Derive the local file path from that URI.
			URI jarUri = p2.getUri("");
			Path jarPath = Paths.get(jarUri); // Works for file:// URIs

			UpdateSiteBundle bundle = parseBundle(p2, jarPath);

			bundlesByName.put(bundle.getSymbolicName(), bundle);
			for (BundlePackage pkg : bundle.getExportedPackages()) {
				packagesToBundles.computeIfAbsent(pkg, key -> new ArrayList<>()).add(bundle);
			}
		}

		return new UpdateSite(repositoryUri.toString(), bundlesByName, packagesToBundles);
	}

	public static UpdateSite build(Path root) throws IOException {
		Map<String, UpdateSiteBundle> bundlesByName = new HashMap<>();
		Map<BundlePackage, List<UpdateSiteBundle>> packagesToBundles = new HashMap<>();
		
		try (var files = Files.walk(root)) {
			files
				.filter(Files::isRegularFile)
				.filter(file -> file.getFileName().toString().endsWith(FileConstants.JAR_FILE_EXTENSION))
				.forEach(file -> {
					var bundle = parseBundle(file);
					bundlesByName.put(bundle.getSymbolicName(), bundle);
					for (BundlePackage pkg : bundle.getExportedPackages()) {
						packagesToBundles.computeIfAbsent(pkg, key -> new ArrayList<>()).add(bundle);
					}
				});
		}
		return new UpdateSite(root.toString(), bundlesByName, packagesToBundles);
	}

	private static UpdateSiteBundle parseBundle(P2Bundle p2, Path jarPath) {
		return parseBundle(jarPath, p2.getUri("").toString());
	}

	private static UpdateSiteBundle parseBundle(Path jarPath) {
		return parseBundle(jarPath, jarPath.toUri().toString());
	}

	private static UpdateSiteBundle parseBundle(Path jarPath, String uri) {
		Map<String, String> headers = ManifestReader.readManifest(jarPath);

		List<BundleDependency> required = ManifestReader.readRequiredBundles(headers);
		List<BundlePackage> imported = ManifestReader.readImportedPackages(headers);
		List<BundlePackage> exported = ManifestReader.readExportedPackages(headers);

		var symbolicName = ManifestReader.readBundleSymbolicName(headers).orElse("");

		var bundle = new UpdateSiteBundle(symbolicName, uri, required, imported, exported);
		bundle.setEntireManifest(headers);
		return bundle;
	}
}