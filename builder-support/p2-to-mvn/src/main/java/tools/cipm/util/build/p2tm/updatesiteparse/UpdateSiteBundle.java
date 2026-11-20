package tools.cipm.util.build.p2tm.updatesiteparse;

import java.util.List;

import tools.cipm.util.build.p2tm.BundleSymbolicNameData;

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
 * In addition to the shared manifest attribute/directive representation
 * inherited from {@link InMemoryManifestRepresentation}, this class exposes the
 * bundle's singleton status via {@link #isSingleton()}.
 * </p>
 *
 * <p>
 * This class is immutable: the list fields are defensive copies taken at
 * construction time, so the instance's state cannot change after creation. It
 * provides value-based {@link #equals(Object)}.
 * </p>
 */
public final class UpdateSiteBundle extends InMemoryManifestRepresentation {
	public final BundleSymbolicNameData symbolicName;
	public final String uri;
	public final List<BundleDependency> requiredBundles;
	public final List<UpdateSitePackage> importedPackages;
	public final List<UpdateSitePackage> exportedPackages;

	public UpdateSiteBundle(BundleSymbolicNameData symbolicName, String uri, List<BundleDependency> requiredBundles,
			List<UpdateSitePackage> importedPackages, List<UpdateSitePackage> exportedPackages) {
		this.symbolicName = symbolicName;
		this.uri = uri;
		this.requiredBundles = List.copyOf(requiredBundles);
		this.importedPackages = List.copyOf(importedPackages);
		this.exportedPackages = List.copyOf(exportedPackages);
	}

	/**
	 * @return Whether this bundle has to be a singleton
	 */
	public boolean isSingleton() {
		return this.symbolicName.isSingleton();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof UpdateSiteBundle other))
			return false;
		return symbolicName.equals(other.symbolicName) && uri.equals(other.uri)
				&& requiredBundles.equals(other.requiredBundles) && importedPackages.equals(other.importedPackages)
				&& exportedPackages.equals(other.exportedPackages);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append("{").append(symbolicName).append('\'').append(", uri='")
				.append(uri).append('\'').append(", requiredBundles=").append(requiredBundles)
				.append(", importedPackages=").append(importedPackages).append(", exportedPackages=")
				.append(exportedPackages).append('}');
		return sb.toString();
	}
}