package tools.cipm.util.build.p2tm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

import tools.cipm.util.build.p2tm.updatesiteparse.BundleDependency;
import tools.cipm.util.build.p2tm.updatesiteparse.UpdateSitePackage;

/**
 * Reads and parses OSGi bundle manifest headers using {@link ManifestElement},
 * which correctly handles quoted commas (e.g. {@code uses:="a,b"}), version
 * ranges, directives and attributes.
 *
 * <p>
 * This is the single source of manifest-parsing logic shared by
 * {@code UpdateSiteBuilder} and {@code MvnRepositoryIndex}, so they stay
 * consistent and avoid hand-rolled (bug-prone) parsing.
 * </p>
 */
public final class ManifestReader {

	private ManifestReader() {
	}

	/**
	 * Parses a manifest file under a JAR file into a map of raw header names to
	 * their values.
	 *
	 * @param jarPath The path to the JAR file, whose manifest should be read
	 * @return a map of header name -> value
	 * @throws IllegalStateException if the manifest cannot be parsed
	 */
	public static Map<String, String> readManifest(Path jarPath) {
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			InputStream is = jar.getInputStream(jar.getJarEntry(FileConstants.JAR_MANIFEST_ENTRY_NAME));
			// ManifestElement.parseBundleManifest fills a Map<String,String> of
			// raw header values (no localization; just raw OSGi headers).
			return ManifestReader.readManifest(is);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Parses a manifest {@link InputStream} into a map of raw header names to their
	 * values.
	 *
	 * @param is the manifest input stream
	 * @return a map of header name -> value
	 * @throws IllegalStateException if the manifest cannot be parsed
	 */
	public static Map<String, String> readManifest(InputStream is) {
		Map<String, String> headers = new LinkedHashMap<>();
		try {
			ManifestElement.parseBundleManifest(is, headers);
		} catch (IOException | BundleException e) {
			throw new IllegalStateException(e);
		}
		return headers;
	}

	/**
	 * Reads and parses the {@code Bundle-SymbolicName} header, returning its value
	 * together with all associated attributes and directives.
	 *
	 * <p>
	 * For a header such as {@code org.foo;singleton:=true}, the returned
	 * {@link BundleSymbolicNameData#symbolicName()} is {@code org.foo} and
	 * {@code directives} contains {@code singleton -> true}.
	 * </p>
	 *
	 * @param manifest the parsed manifest headers
	 * @return the parsed symbolic-name data, or empty if the header is absent or
	 *         blank
	 */
	public static Optional<BundleSymbolicNameData> readBundleSymbolicName(Map<String, String> manifest) {
		String value = manifest.get(OsgiHeaders.BUNDLE_SYMBOLIC_NAME);
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		ManifestElement[] elements = parseHeader(OsgiHeaders.BUNDLE_SYMBOLIC_NAME, value);
		if (elements == null || elements.length == 0) {
			return Optional.empty();
		}
		// There should always only be up to one element, hence no iteration over all
		// elements.
		ManifestElement el = elements[0];
		var bsnd = new BundleSymbolicNameData(el.getValue(), collectAttributes(el), collectDirectives(el));
		bsnd.setEntireManifest(manifest);
		return Optional.of(bsnd);
	}

	/**
	 * Parses the {@code Require-Bundle} header into {@link BundleDependency}s.
	 *
	 * @param manifest the parsed manifest headers
	 * @return the list of required-bundle dependencies (never null)
	 */
	public static List<BundleDependency> readRequiredBundles(Map<String, String> manifest) {
		String value = manifest.get(OsgiHeaders.REQUIRE_BUNDLE);
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<BundleDependency> result = new ArrayList<>();
		for (ManifestElement el : parseHeader(OsgiHeaders.REQUIRE_BUNDLE, value)) {
			var dep = parseBundleDependency(el);
			dep.setEntireManifest(manifest);
			result.add(dep);
		}
		return result;
	}

	/**
	 * Parses the {@code Import-Package} header into {@link UpdateSitePackage}s.
	 *
	 * @param manifest the parsed manifest headers
	 * @return the list of imported-package requirements (never null)
	 */
	public static List<UpdateSitePackage> readImportedPackages(Map<String, String> manifest) {
		String value = manifest.get(OsgiHeaders.IMPORT_PACKAGE);
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<UpdateSitePackage> result = new ArrayList<>();
		for (ManifestElement el : parseHeader(OsgiHeaders.IMPORT_PACKAGE, value)) {
			var pac = parsePackage(el);
			pac.setEntireManifest(manifest);
			result.add(pac);
		}
		return result;
	}

	/**
	 * Parses the {@code Export-Package} header into {@link UpdateSitePackage}s.
	 *
	 * <p>
	 * Exports are not "optional" in the same sense as imports, so {@code optional}
	 * is set to {@code false}.
	 * </p>
	 *
	 * @param manifest the parsed manifest headers
	 * @return the list of exported-package requirements (never null)
	 */
	public static List<UpdateSitePackage> readExportedPackages(Map<String, String> manifest) {
		String value = manifest.get(OsgiHeaders.EXPORT_PACKAGE);
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<UpdateSitePackage> result = new ArrayList<>();
		for (ManifestElement el : parseHeader(OsgiHeaders.EXPORT_PACKAGE, value)) {
			var pac = parsePackage(el);
			pac.setEntireManifest(manifest);
			result.add(pac);
		}
		return result;
	}

	private static UpdateSitePackage parsePackage(ManifestElement el) {
		var packageName = el.getValue();
		var attributes = collectAttributes(el);
		var directives = collectDirectives(el);
		return new UpdateSitePackage(packageName, attributes, directives);
	}

	private static BundleDependency parseBundleDependency(ManifestElement el) {
		var bundleName = el.getValue();
		var attributes = collectAttributes(el);
		var directives = collectDirectives(el);
		return new BundleDependency(bundleName, attributes, directives);
	}

	/**
	 * Convenience for callers that need only the plain exported package name
	 * strings (e.g. to build package-level Coordinates).
	 *
	 * @param manifest the parsed manifest headers
	 * @return the list of exported package name strings (never null)
	 */
	public static List<String> readExportedPackageNames(Map<String, String> manifest) {
		List<UpdateSitePackage> reqs = readExportedPackages(manifest);
		List<String> names = new ArrayList<>(reqs.size());
		for (UpdateSitePackage req : reqs) {
			names.add(req.packageName);
		}
		return names;
	}

	private static ManifestElement[] parseHeader(String header, String value) {
		try {
			return ManifestElement.parseHeader(header, value);
		} catch (BundleException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Collects all attribute name/value pairs from the element.
	 */
	private static Map<String, String> collectAttributes(ManifestElement el) {
		Map<String, String> result = new LinkedHashMap<>();
		Enumeration<String> keys = el.getKeys();
		if (keys != null) {
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				String value = el.getAttribute(key);
				if (value != null) {
					result.put(key, value);
				}
			}
		}
		return result;
	}

	/**
	 * Collects all directive name/value pairs from the element.
	 */
	private static Map<String, String> collectDirectives(ManifestElement el) {
		Map<String, String> result = new LinkedHashMap<>();
		Enumeration<String> keys = el.getDirectiveKeys();
		if (keys != null) {
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				String value = el.getDirective(key);
				if (value != null) {
					result.put(key, value);
				}
			}
		}
		return result;
	}
}