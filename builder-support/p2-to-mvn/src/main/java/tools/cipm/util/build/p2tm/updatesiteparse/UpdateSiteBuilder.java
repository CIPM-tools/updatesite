package tools.cipm.util.build.p2tm.updatesiteparse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.openntf.maven.p2.model.P2Bundle;
import org.openntf.maven.p2.model.P2Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.cipm.util.build.p2tm.BundleSymbolicNameData;
import tools.cipm.util.build.p2tm.FileConstants;
import tools.cipm.util.build.p2tm.ManifestReader;

/**
 * Builds a complete {@link UpdateSite} by combining p2-layout-resolver
 * (UpdateSiteBundle enumeration) with org.eclipse.osgi.util.ManifestElement
 * (accurate manifest parsing).
 */
public final class UpdateSiteBuilder {
	private static final Logger logger = LoggerFactory.getLogger(UpdateSiteBuilder.class);

	private final HttpClient http;

	public UpdateSiteBuilder() {
		this.http = HttpClient.newHttpClient();
	}

	/**
	 * Builds the in-memory representation of the given p2 update site.
	 *
	 * <p>
	 * FIXME Not tested
	 *
	 * @param repositoryUri the p2 repository URI
	 * @return the fully parsed {@link UpdateSite}
	 */
	public UpdateSite build(String repositoryUri) throws IOException, InterruptedException {
		P2Repository repo = P2Repository.getInstance(URI.create(repositoryUri), logger);

		Map<String, UpdateSiteBundle> bundlesByName = new LinkedHashMap<>();
		Map<UpdateSitePackage, List<UpdateSiteBundle>> packagesToBundles = new LinkedHashMap<>();

		for (P2Bundle p2 : repo.getBundles()) {
			UpdateSiteBundle UpdateSiteBundle = parseRemoteBundle(p2);
			bundlesByName.put(UpdateSiteBundle.symbolicName.symbolicName, UpdateSiteBundle);

			for (UpdateSitePackage pkg : UpdateSiteBundle.exportedPackages) {
				packagesToBundles.computeIfAbsent(pkg, k -> new ArrayList<>()).add(UpdateSiteBundle);
			}
		}

		return new UpdateSite(repositoryUri, bundlesByName, packagesToBundles);
	}

	/**
	 * Builds the in-memory representation of a LOCAL update-site clone.
	 *
	 * @param localCloneRoot the filesystem path to the cloned update site
	 *                       (containing artifacts.xml/jar and plugins/)
	 */
	public UpdateSite buildLocal(String localCloneRoot) throws IOException {
		// P2Repository handles file:// transparently via P2Util (non-HTTP branch).
		var repositoryUri = Paths.get(localCloneRoot).toAbsolutePath().toUri();
		P2Repository repo = P2Repository.getInstance(repositoryUri, logger);

		Map<String, UpdateSiteBundle> bundlesByName = new LinkedHashMap<>();
		Map<UpdateSitePackage, List<UpdateSiteBundle>> packagesToBundles = new LinkedHashMap<>();

		for (P2Bundle p2 : repo.getBundles()) {
			UpdateSiteBundle bundle = parseLocalBundle(p2, localCloneRoot);
			bundlesByName.put(bundle.symbolicName.symbolicName, bundle);
			for (UpdateSitePackage pkg : bundle.exportedPackages) {
				packagesToBundles.computeIfAbsent(pkg, k -> new ArrayList<>()).add(bundle);
			}
		}

		return new UpdateSite(repositoryUri.toString(), bundlesByName, packagesToBundles);
	}

	/**
	 * FIXME Not tested
	 */
	private UpdateSiteBundle parseRemoteBundle(P2Bundle p2) throws IOException, InterruptedException {
		String uri = p2.getUri("").toString();
		Path tmp = downloadToTemp(uri);
		return parseBundle(p2, tmp, true);
	}

	private static UpdateSiteBundle parseBundle(P2Bundle p2, Path jarPath, boolean deleteJARFile) {
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			InputStream is = jar.getInputStream(jar.getJarEntry(FileConstants.JAR_MANIFEST_ENTRY_NAME));
			// Use ManifestReader.readManifest(is), since the finally block is crucial for
			// deleting the locally cloned JAR files after exceptions
			Map<String, String> headers = ManifestReader.readManifest(is);

			List<BundleDependency> required = ManifestReader.readRequiredBundles(headers);
			List<UpdateSitePackage> imported = ManifestReader.readImportedPackages(headers);
			List<UpdateSitePackage> exported = ManifestReader.readExportedPackages(headers);

			var symbolicNameData = ManifestReader.readBundleSymbolicName(headers)
					.orElse(new BundleSymbolicNameData(p2.getId()));

			var bundle = new UpdateSiteBundle(symbolicNameData, p2.getUri("").toString(), required, imported, exported);
			bundle.setEntireManifest(headers);
			return bundle;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			if (deleteJARFile) {
				try {
					Files.deleteIfExists(jarPath);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

	/**
	 * Reads a bundle's JAR directly from the local plugins/ directory and parses
	 * its manifest via ManifestElement.
	 */
	private UpdateSiteBundle parseLocalBundle(P2Bundle p2, String localCloneRoot) throws IOException {
		// P2Bundle.getUri("") for a file:// base yields
		// file:///<root>/plugins/<id>_<version>.jar
		// Derive the local file path from that URI.
		URI jarUri = p2.getUri("");
		Path jarPath = Paths.get(jarUri); // works for file:// URIs
		return parseBundle(p2, jarPath, false);
	}

	/**
	 * FIXME Not tested
	 */
	private Path downloadToTemp(String uri) throws IOException, InterruptedException {
		Path tmp = Files.createTempFile("p2bundle", FileConstants.JAR_FILE_EXTENSION);
		HttpResponse<Path> resp = http.send(HttpRequest.newBuilder(URI.create(uri)).GET().build(),
				HttpResponse.BodyHandlers.ofFile(tmp));
		if (resp.statusCode() != 200) {
			throw new IOException("Failed to download " + uri + ": HTTP " + resp.statusCode());
		}
		return tmp;
	}
}