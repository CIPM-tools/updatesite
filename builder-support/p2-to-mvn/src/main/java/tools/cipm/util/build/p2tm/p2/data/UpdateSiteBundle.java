package tools.cipm.util.build.p2tm.p2.data;

import java.util.List;

/**
 * A single OSGi bundle from a parsed update site, together with its manifest
 * information.
 *
 * <p>
 * This class represents one bundle artifact found in an update site, capturing
 * both its identity (the {@link BundleSymbolicNameData} from the
 * {@code Bundle-SymbolicName} header) and its inter-bundle relationships:
 * </p>
 * <ul>
 * <li>the bundles it {@code Require-Bundle}s (see
 * {@link #requiredBundles}),</li>
 * <li>the packages it {@code Import-Package}s (see {@link #importedPackages}),
 * and</li>
 * <li>the packages it {@code Export-Package}s (see
 * {@link #exportedPackages}),</li>
 * </ul>
 * together with the location of its artifact (see {@link #uri}).
 *
 * <p>
 * This class is immutable: the list fields are defensive copies taken at
 * construction time, so the instance's state cannot change after creation.
 * </p>
 */
public final class UpdateSiteBundle extends InMemoryManifestRepresentation {
	private final String symbolicName;
	private final String uri;
	private final List<BundleDependency> requiredBundles;
	private final List<BundlePackage> importedPackages;
	private final List<BundlePackage> exportedPackages;

	public UpdateSiteBundle(String symbolicName, String uri, List<BundleDependency> requiredBundles,
			List<BundlePackage> importedPackages, List<BundlePackage> exportedPackages) {
		this.symbolicName = symbolicName;
		this.uri = uri;
		this.requiredBundles = List.copyOf(requiredBundles);
		this.importedPackages = List.copyOf(importedPackages);
		this.exportedPackages = List.copyOf(exportedPackages);
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getUri() {
		return uri;
	}

	public List<BundleDependency> getRequiredBundles() {
		return requiredBundles;
	}

	public List<BundlePackage> getImportedPackages() {
		return importedPackages;
	}

	public List<BundlePackage> getExportedPackages() {
		return exportedPackages;
	}
}